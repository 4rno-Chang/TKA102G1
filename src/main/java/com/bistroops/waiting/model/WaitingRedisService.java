package com.bistroops.waiting.model;

import java.time.LocalDate;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;

import com.bistroops.sms.SmsService;
import java.time.ZoneId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

@Service
public class WaitingRedisService {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Taipei");

	private static final Set<Integer> TABLE_TYPES = Set.of(2, 4, 6);

	private static final Set<String> ACTIVE_STATUSES = Set.of("WAITING", "NOTIFIED");

	private static final Set<String> ALL_STATUSES = Set.of("WAITING", "NOTIFIED", "SEATED", "MISSED", "CANCELLED");

	private final StringRedisTemplate redisTemplate;
	
	@Autowired
	private SmsService smsService;
	
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WaitingRedisService.class);

	private long lastSweepWarningAt;
	private final DefaultRedisScript<String> operationScript;

	private final Gson gson = new Gson();

	public WaitingRedisService(StringRedisTemplate redisTemplate) {

		this.redisTemplate = redisTemplate;

		this.operationScript = new DefaultRedisScript<>();

		this.operationScript.setLocation(new ClassPathResource("redis/waiting/take-ticket.lua"));

		this.operationScript.setResultType(String.class);
	}

	// 電話必填，姓名選填，不依赖會員登入。
	public WaitingTicket takeTicket(Integer tableType, String phone, String name, String note) {

		checkTableType(tableType);

		String normalizedPhone = trim(phone);
		String normalizedName = trim(name);
		String normalizedNote = normalizeNote(note);

		if (!normalizedPhone.matches("09[0-9]{8}")) {
			throw new IllegalArgumentException("請輸入09開頭的10碼手機號碼");
		}

		if (length(normalizedName) > 30) {
			throw new IllegalArgumentException("姓名最多30個字元");
		}

		String ticketId = UUID.randomUUID().toString();
		String accessToken = UUID.randomUUID().toString();

		String json = execute("TAKE", tableType, ticketId, accessToken, normalizedPhone, normalizedName,
				normalizedNote);

		WaitingTicket ticket = parseTicket(json);

		if (!ticketId.equals(ticket.ticketId()) || !accessToken.equals(ticket.accessToken())
				|| ticket.tableType() != tableType) {

			throw invalidResult();
		}

		return ticket;
	}

	// 後台使用：取得指定桌型的今日紀錄。
	// 這個結果包含個人資料與憑證，不可公開為前台 JSON。
	public List<WaitingTicket> getTodayTickets(Integer tableType) {

		String json = execute("LIST", tableType, "", "", "", "", "");

		try {
			WaitingTicket[] tickets = gson.fromJson(json, WaitingTicket[].class);

			if (tickets == null) {
				throw invalidResult();
			}

			for (WaitingTicket ticket : tickets) {
				validateTicket(ticket);

				if (ticket.tableType() != tableType) {
					throw invalidResult();
				}
			}

			return Arrays.stream(tickets).sorted(Comparator.comparingLong(WaitingTicket::sequence)).toList();

		} catch (JsonParseException ex) {
			throw new WaitingUnavailableException("候位資料無法讀取，請洽現場人員", ex);
		}
	}

	// 客人查看自己的號碼牌。
	public WaitingTicket getTicket(String date, Integer tableType, String ticketId, String accessToken) {

		requireToday(date);
		checkTicketId(ticketId);
		checkAccessToken(accessToken);

		return parseTicket(execute("GET", tableType, ticketId, accessToken, "", "", ""));
	}

	// 保留舊呼叫方式：人工叫號，不發簡訊。
	public WaitingTicket notifyNext(Integer tableType) {
	    return notifyWaiting(tableType, "", false);
	}

	// 保留舊呼叫方式：人工叫號，不發簡訊。
	public WaitingTicket notifyWaiting(
	        Integer tableType, String ticketId) {

	    checkTicketId(ticketId);

	    return notifyWaiting(tableType, ticketId, false);
	}

	// 新後台使用：先依選擇處理簡訊，再啟動Redis叫號倒數。
	public WaitingTicket notifyWaiting(
	        Integer tableType,
	        String expectedTicketId,
	        boolean sendSms) {

	    checkTableType(tableType);

	    String ticketId = trim(expectedTicketId);

	    if (!ticketId.isEmpty()) {
	        checkTicketId(ticketId);
	    }

	    LocalDate businessDate = LocalDate.now(BUSINESS_ZONE);

	    String lockKey = "bistroops:waiting:notify-lock:"
	            + businessDate + ":" + tableType;

	    String lockOwner = UUID.randomUUID().toString();

	    boolean locked;

	    try {
	        locked = Boolean.TRUE.equals(
	                redisTemplate.opsForValue().setIfAbsent(
	                        lockKey,
	                        lockOwner,
	                        Duration.ofMinutes(2)));

	    } catch (DataAccessException ex) {
	        throw new WaitingUnavailableException(
	                "目前無法叫號，請稍後再試",
	                ex);
	    }

	    if (!locked) {
	        throw new IllegalArgumentException(
	                "這個桌型正在處理叫號，請稍後重新整理");
	    }

	    try {
	        WaitingTicket next = getTodayTickets(tableType).stream()
	                .filter(ticket -> "WAITING".equals(ticket.status()))
	                .min(Comparator.comparingLong(WaitingTicket::sequence))
	                .orElseThrow(() -> new IllegalArgumentException(
	                        "此桌型目前沒有可叫號的候位"));

	        if (!ticketId.isEmpty()
	                && !ticketId.equals(next.ticketId())) {
	            throw new IllegalArgumentException(
	                    "候位順序已更新，請重新整理後再叫號");
	        }

	        if (!businessDate.equals(LocalDate.now(BUSINESS_ZONE))) {
	            throw new IllegalArgumentException(
	                    "日期已切換，請重新整理");
	        }

	        if (sendSms) {
	            // 如果發送拋出例外，下面的Redis叫號不會執行，
	            // 號碼牌仍維持候位中，也不開始一分鐘計時。
	            smsService.send(
	                    next.phone(),
	                    "【Bistroops】您的候位號碼 "
	                            + next.getTicketDisplay()
	                            + " 已到號，請於十分鐘內向現場人員報到。"
	                            + "過號請重新掃描 QR Code 取號。");
	        }

	        if (!businessDate.equals(LocalDate.now(BUSINESS_ZONE))) {
	            throw new IllegalArgumentException(
	                    sendSms
	                            ? "簡訊已處理，但日期已切換，未完成叫號，請洽現場處理"
	                            : "日期已切換，請重新整理");
	        }

	        try {
	            // Lua會在這裡設定NOTIFIED及60秒期限。
	            return parseTicket(execute(
	                    "NOTIFY",
	                    tableType,
	                    next.ticketId(),
	                    "", "", "", ""));

	        } catch (RuntimeException ex) {
	            if (sendSms) {
	                throw new WaitingUnavailableException(
	                        "簡訊已處理，但叫號結果未確認；"
	                                + "請重新整理確認，不要直接重送簡訊",
	                        ex);
	            }

	            throw ex;
	        }

	    } finally {
	        // 只解除自己取得的鎖。
	        DefaultRedisScript<Long> unlockScript =
	                new DefaultRedisScript<>(
	                        "if redis.call('GET', KEYS[1]) == ARGV[1] then "
	                                + "return redis.call('DEL', KEYS[1]) "
	                                + "else return 0 end",
	                        Long.class);

	        try {
	            redisTemplate.execute(
	                    unlockScript,
	                    List.of(lockKey),
	                    lockOwner);

	        } catch (RuntimeException ex) {
	            log.warn("叫號鎖暫時無法解除，將由到期機制清除");
	        }
	    }
	}

	// 員工手動標記過號。
	public WaitingTicket missWaiting(Integer tableType, String ticketId) {

		checkTicketId(ticketId);

		return parseTicket(execute("MISS", tableType, ticketId, "", "", "", ""));
	}

	// 員工標記入座。
	// 不會同時分配實際桌號或建立訂單。
	public WaitingTicket seatWaiting(Integer tableType, String ticketId) {

		checkTicketId(ticketId);

		return parseTicket(execute("SEAT", tableType, ticketId, "", "", "", ""));
	}

	public WaitingTicket cancelByStaff(Integer tableType, String ticketId) {

		checkTicketId(ticketId);

		return parseTicket(execute("CANCEL_STAFF", tableType, ticketId, "", "", "", ""));
	}

	// 客人取消自己的候位，必須持有憑證。
	public WaitingTicket cancelByTicket(String date, Integer tableType, String ticketId, String accessToken) {

		requireToday(date);
		checkTicketId(ticketId);
		checkAccessToken(accessToken);

		return parseTicket(execute("CANCEL_CLIENT", tableType, ticketId, accessToken, "", "", ""));
	}

	public WaitingTicket updateCommentByStaff(Integer tableType, String ticketId, String note) {

		checkTicketId(ticketId);

		return parseTicket(execute("COMMENT", tableType, ticketId, "", "", "", normalizeNote(note)));
	}

	// 前台公開資訊，不含電話、姓名與憑證。
	public Map<String, Object> getPublicSummary(Integer tableType) {

		List<WaitingTicket> tickets = getTodayTickets(tableType);

		long waitingCount = tickets.stream().filter(this::isActive).count();

		List<String> notifiedNumbers = tickets.stream().filter(ticket -> "NOTIFIED".equals(ticket.status()))
				.map(WaitingTicket::getTicketDisplay).toList();

		return Map.of("tableType", tableType, "waitingCount", waitingCount, "notifiedNumbers", notifiedNumbers);
	}

	// 前面尚未結束的組數，包含已叫號但尚未入座的組別。
	// 呼叫端必須先驗證這張號碼牌的操作憑證。
	public long countAhead(WaitingTicket ticket) {

		validateTicket(ticket);
		requireToday(ticket.date());

		if (!isActive(ticket)) {
			return 0;
		}

		return getTodayTickets(ticket.tableType()).stream().filter(this::isActive)
				.filter(other -> other.sequence() < ticket.sequence()).count();
	}

	public boolean isActive(WaitingTicket ticket) {

		return ticket != null && ticket.status() != null && ACTIVE_STATUSES.contains(ticket.status());
	}

	private String execute(String operation, Integer tableType, String ticketId, String accessToken, String phone,
			String name, String note) {

		checkTableType(tableType);

		LocalDate date = LocalDate.now(BUSINESS_ZONE);

		long startAt = date.atStartOfDay(BUSINESS_ZONE).toEpochSecond();

		long expiresAt = date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toEpochSecond();

		String result;

		try {
			result = redisTemplate.execute(operationScript, keys(date, tableType), operation, date.toString(),
					tableType.toString(), Long.toString(startAt), Long.toString(expiresAt), trim(ticketId),
					trim(accessToken), trim(phone), trim(name), trim(note));

		} catch (DataAccessException ex) {
			throw new WaitingUnavailableException("候位服務暫時無法使用，請洽現場人員；" + "本次操作結果尚未確認，請勿連續重複送出", ex);
		}

		if (result == null) {
			throw invalidResult();
		}

		if (result.startsWith("OK|")) {
			return result.substring(3);
		}

		switch (result) {
		case "DUPLICATE" -> throw new IllegalArgumentException("此電話在所選桌型已有有效候位");

		case "DATE_CHANGED" -> throw new IllegalArgumentException("候位日期已切換，請重新掃描QR Code");

		case "NO_WAITING" -> throw new IllegalArgumentException("此桌型目前沒有可叫號的候位");

		case "OUT_OF_ORDER" -> throw new IllegalArgumentException("請依取號順序叫號");

		case "NOT_NOTIFIED" -> throw new IllegalArgumentException("請先叫號，再標記過號");

		case "NOT_FOUND" -> throw new IllegalArgumentException("找不到當日號碼牌，可能已過期");

		case "FORBIDDEN" -> throw new IllegalArgumentException("無法查看或操作這張號碼牌");

		case "ENDED" -> throw new IllegalArgumentException("這張號碼牌已結束，不能再次操作");

		case "COUNTER_LIMIT" -> throw new IllegalArgumentException("今日候位號碼已達上限");

		default -> throw new WaitingUnavailableException("候位資料設定異常，請洽現場人員", null);
		}
	}

	private WaitingTicket parseTicket(String json) {

		try {
			WaitingTicket ticket = gson.fromJson(json, WaitingTicket.class);

			validateTicket(ticket);

			return ticket;

		} catch (JsonParseException ex) {
			throw new WaitingUnavailableException("號碼牌資料無法讀取，請洽現場人員", ex);
		}
	}

	private void validateTicket(WaitingTicket ticket) {

		if (ticket == null || ticket.ticketId() == null || ticket.ticketId().isBlank() || ticket.accessToken() == null
				|| ticket.accessToken().isBlank() || ticket.date() == null || ticket.phone() == null
				|| ticket.status() == null || !ALL_STATUSES.contains(ticket.status()) || ticket.sequence() <= 0
				|| !TABLE_TYPES.contains(ticket.tableType())) {

			throw invalidResult();
		}
	}

	private List<String> keys(LocalDate date, Integer tableType) {

		String prefix = "bistroops:waiting:{" + date + ":" + tableType + "}:";

		return List.of(prefix + "sequence", prefix + "tickets", prefix + "phones", prefix + "queue");
	}

	private void requireToday(String date) {

		if (!LocalDate.now(BUSINESS_ZONE).toString().equals(date)) {

			throw new IllegalArgumentException("號碼牌已過期，請重新掃描QR Code");
		}
	}

	private void checkTableType(Integer tableType) {

		if (tableType == null || !TABLE_TYPES.contains(tableType)) {

			throw new IllegalArgumentException("請選擇2、4或6人桌");
		}
	}

	private void checkTicketId(String ticketId) {

		if (ticketId == null || ticketId.isBlank()) {
			throw new IllegalArgumentException("缺少號碼牌識別碼");
		}
	}

	private void checkAccessToken(String accessToken) {

		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalArgumentException("缺少號碼牌操作憑證");
		}
	}

	private String normalizeNote(String note) {

		String value = trim(note);

		if (length(value) > 20) {
			throw new IllegalArgumentException("備註最多20個字元");
		}

		return value;
	}

	private WaitingUnavailableException invalidResult() {

		return new WaitingUnavailableException("候位操作結果無法確認，請洽現場人員", null);
	}

	private String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private int length(String value) {
		return value.codePointCount(0, value.length());
	}

	@org.springframework.scheduling.annotation.Scheduled(fixedDelay = 1000)
	public void expireNotifiedTickets() {

		try {
			for (Integer tableType : List.of(2, 4, 6)) {
				execute("SWEEP", tableType, "", "", "", "", "");
			}

		} catch (WaitingUnavailableException ex) {
			long now = System.currentTimeMillis();

			if (now - lastSweepWarningAt >= 60000) {
				lastSweepWarningAt = now;
				log.warn("候位自動過號暫停：{}", ex.getMessage());
			}
		}
	}
}