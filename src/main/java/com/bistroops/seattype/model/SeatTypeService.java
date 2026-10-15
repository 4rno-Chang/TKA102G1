package com.bistroops.seattype.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.member.model.MemberVO;
import com.bistroops.orders.model.OrdersService;
import com.bistroops.orders.model.OrdersVO;
import com.bistroops.reservation.model.ReservationService;
import com.bistroops.reservation.model.ReservationVO;
import com.bistroops.seat.model.SeatVO;
import com.bistroops.waiting.model.WaitingRedisService;
import com.bistroops.waiting.model.WaitingTicket;
import com.bistroops.waiting.model.WaitingUnavailableException;

import jakarta.persistence.EntityNotFoundException;

/*
 * 入座管理：所有計算都在這裡做，算好之後放進一個 Map 給 staff/seating.html 顯示（Thymeleaf 不做計算）
 *
 * 直接注入組員的 Service 讀資料，只有兩個地方會寫組員的資料（都在入座的同一個交易裡）：
 *   · 訂位者入座：訂位改成「實到」（呼叫 CJ 的 ReservationService.markArrivedByStaff）
 *   · 候位入座：呼叫 CJ 的 WaitingRedisService.seatWaiting，Redis 號碼牌改成「已入座」
 * 不記錄「哪桌坐誰」；有訂單的桌子才從訂單的會員（OrdersVO.getMember()）取客人資料，只是顯示用。
 *
 * 時間一律用「當天的第幾分鐘」計算（例如 17:30 = 1050），seat.seat_time 只有時間、沒有日期。
 * 注意：Thymeleaf 讀 Map 時，key 不存在會出錯，所以每個 Map 都要放齊畫面會讀的 key（值可以是 null）。
 */
@Service
@Transactional(readOnly = true)
public class SeatTypeService {

	public static final int EST = 90; // 固定用餐時間（分鐘）
	private static final int RANGE = 4 * 60; // 時間軸畫現在前後各幾分鐘
	public static final List<Integer> TYPES = List.of(2, 4, 6);
	private static final String RSV_PENDING = "預約";
	private static final String RSV_ARRIVED = "實到";
	private static final String RSV_CANCELLED = "取消";
	private static final String RSV_NO_SHOW = "未到";

	// 平面圖上桌子的位置（桌子中心，單位是 %）。寫死在這裡，不存資料庫；不在表上的桌號不畫（桌位管理之後再做）
	private static final Map<String, int[]> LAYOUT = Map.ofEntries(Map.entry("A01", new int[] { 9, 18 }),
			Map.entry("A02", new int[] { 9, 36 }), Map.entry("A03", new int[] { 9, 54 }),
			Map.entry("A04", new int[] { 22, 18 }), Map.entry("A05", new int[] { 22, 36 }),
			Map.entry("A06", new int[] { 22, 54 }), Map.entry("A07", new int[] { 42, 18 }),
			Map.entry("A08", new int[] { 54, 18 }), Map.entry("A09", new int[] { 66, 18 }),
			Map.entry("A10", new int[] { 42, 52 }), Map.entry("B01", new int[] { 54, 37 }),
			Map.entry("B02", new int[] { 66, 37 }), Map.entry("B03", new int[] { 54, 55 }),
			Map.entry("B04", new int[] { 66, 55 }), Map.entry("B05", new int[] { 42, 35 }),
			Map.entry("C01", new int[] { 87, 22 }), Map.entry("C02", new int[] { 87, 58 }));

	private final SeatingSeatService seatService;
	private final OrdersService ordersService;
	private final ReservationService reservationService;
	private final WaitingRedisService waitingRedisService;

	public SeatTypeService(SeatingSeatService seatService, OrdersService ordersService,
			ReservationService reservationService, WaitingRedisService waitingRedisService) {

		this.seatService = seatService;
		this.ordersService = ordersService;
		this.reservationService = reservationService;
		this.waitingRedisService = waitingRedisService;
	}

	/*
	 * =====================================================================
	 * 一、讀資料、計算
	 * =====================================================================
	 */

	// 一次讀進今天需要的資料，後面的計算都用這一份
	private class Snapshot {
		final LocalDateTime businessNow = LocalDateTime.now(java.time.ZoneId.of("Asia/Taipei"));

		final LocalDate today = businessNow.toLocalDate();
		final int now = minuteOf(businessNow.toLocalTime());
		final List<SeatVO> seats = seatService.getAll();
		final List<OrdersVO> orders = ordersService.search(null, null, today);
		final List<ReservationVO> reservations = reservationService.search(today, null);
		final Map<Integer, List<WaitingTicket>> waiting = new LinkedHashMap<>();

		boolean waitingAvailable = true;

		Snapshot() {
			try {
				for (Integer type : TYPES) {
					List<WaitingTicket> tickets = waitingRedisService.getTodayTickets(type).stream()
							.filter(waitingRedisService::isActive)
							.sorted(Comparator.comparingLong(WaitingTicket::sequence)).toList();

					waiting.put(type, tickets);
				}

			} catch (WaitingUnavailableException ex) {
				waitingAvailable = false;

				// 不使用讀取到一半的資料。
				waiting.clear();

				for (Integer type : TYPES) {
					waiting.put(type, List.of());
				}
			}
		}

		List<SeatVO> seatsOf(int type) {
			return seats.stream().filter(s -> s.getSeatTypeNo() == type).toList();
		}

		// 入座時間比現在還晚：多半是之前沒清桌（seat_time 沒有日期）
		boolean isAnomaly(SeatVO s) {
			return SeatingSeatService.isInUse(s) && s.getSeatTime() != null && minuteOf(s.getSeatTime()) > now;
		}

		boolean isSeated(SeatVO s) {
			return SeatingSeatService.isInUse(s) && s.getSeatTime() != null && !isAnomaly(s);
		}

		Integer minsOf(SeatVO s) {
			return isSeated(s) ? now - minuteOf(s.getSeatTime()) : null;
		}

		boolean isLate(SeatVO s) {
			Integer mins = minsOf(s);
			return mins != null && mins >= EST;
		}

		// 現在這組客人的訂單：同一桌、今天、下單時間不早於入座時間（取消的也算在內，顯示時再分）
		List<OrdersVO> currentOrders(SeatVO s) {
			if (!isSeated(s)) {
				return List.of();
			}
			LocalDateTime seatedAt = today.atTime(s.getSeatTime());
			return orders.stream().filter(o -> s.getSeatNo().equals(o.getSeatNo().getSeatNo()))
					.filter(o -> o.getOrdersTime() != null && !o.getOrdersTime().isBefore(seatedAt))
					.sorted(Comparator.comparing(OrdersVO::getOrdersTime)).toList();
		}

		List<OrdersVO> activeOrders(SeatVO s) {
			return currentOrders(s).stream().filter(o -> !isCancelled(o)).toList();
		}

		List<OrdersVO> unpaidOrders(SeatVO s) {
			return activeOrders(s).stream().filter(SeatTypeService::isUnpaid).toList();
		}

		boolean isPaid(SeatVO s) {
			return !activeOrders(s).isEmpty() && unpaidOrders(s).isEmpty();
		}

		List<ReservationVO> pendingOf(int type) {
			return reservations.stream()
					.filter(r -> r.getSeatType().getSeatTypeNo() == type && RSV_PENDING.equals(r.getRsvStatus()))
					.sorted(Comparator.comparing(r -> r.getRsvDt().getRsvDtDatetime())).toList();
		}

		/*
		 * 保留桌數 需保留 = 這個桌型、訂位時間在 [現在, 現在 + 90 分鐘) 之內、還是「預約」的筆數 現場還能坐 = 空桌 − 需保留（最少
		 * 0）；訂位時間一過還沒來 = 逾時未到，不再保留
		 */
		Quota quota(int type) {
			List<SeatVO> list = seatsOf(type);
			int free = (int) list.stream().filter(s -> SeatingSeatService.FREE.equals(s.getSeatStatus())).count();
			int off = (int) list.stream().filter(s -> SeatingSeatService.OFF.equals(s.getSeatStatus())).count();
			List<ReservationVO> pending = pendingOf(type);
			List<ReservationVO> upcoming = pending.stream().filter(r -> slotOf(r) >= now && slotOf(r) < now + EST)
					.toList();
			int noShow = (int) pending.stream().filter(r -> slotOf(r) < now).count();
			Integer nextSlot = upcoming.stream().map(SeatTypeService::slotOf).min(Integer::compare).orElse(null);
			int need = upcoming.size();
			return new Quota(type, list.size(), off, free, need, noShow, waiting.get(type).size(),
					Math.max(free - need, 0), Math.max(need - free, 0), nextSlot);
		}

		/*
		 * 時間軸：這個時段、這個桌型夠不夠桌子 需要 = 這個時段還是「預約」的筆數 預計空出 = 現在的空桌 + 用餐中、預估在時段前離開的桌子（入座 + 90
		 * 分鐘 ≤ 時段）
		 */
		int[] slotCheck(int type, int slot) {
			List<SeatVO> list = seatsOf(type);
			int need = (int) pendingOf(type).stream().filter(r -> slotOf(r) == slot).count();
			int free = (int) list.stream().filter(s -> SeatingSeatService.FREE.equals(s.getSeatStatus())).count();
			int freeing = (int) list.stream().filter(s -> isSeated(s) && minuteOf(s.getSeatTime()) + EST <= slot)
					.count();
			return new int[] { need, free + freeing, (slot >= now && need > free + freeing) ? 1 : 0 };
		}
	}

	private record Quota(int type, int total, int off, int free, int need, int noShow, int waiting, int walkLeft,
			int shortage, Integer nextSlot) {
	}

	/*
	 * =====================================================================
	 * 二、整頁的資料（Controller 放進 Model 的 page） view：floor 平面圖／timeline
	 * 時間軸；filter（桌型）：all、2、4、6； state（狀態，只有平面圖用）：all、free 可使用、use 使用中、off 停用、late
	 * 超時（用餐超過 90 分鐘）、anomaly 時間異常； 桌型和狀態同時生效（兩個都符合的桌子才是亮的）；selected：選到的桌號
	 * focus（只有時間軸用，點上方的卡片）：all、busy 用餐中、rsv 90 分鐘內的預約、warn 須注意；和桌型疊加
	 * =====================================================================
	 */
	public static final List<String> STATES = List.of("free", "use", "off", "late", "anomaly");
	// 平面圖說明列（狀態篩選）的每一項：state 值、圖示的 class、文字
	private static final List<Map<String, Object>> STATE_OPTIONS = List.of(
			Map.of("value", "free", "cls", "st-free", "label", "可使用"),
			Map.of("value", "use", "cls", "st-use", "label", "使用中"),
			Map.of("value", "off", "cls", "st-off", "label", "停用"),
			Map.of("value", "late", "cls", "lg-late", "label", "超時"),
			Map.of("value", "anomaly", "cls", "lg-warn", "label", "時間異常"));

	public static final List<String> FOCUSES = List.of("busy", "rsv", "warn");

	public Map<String, Object> getPage(String view, String filter, String state, String focus, String selected) {
		Snapshot snap = new Snapshot();
		boolean timeline = "timeline".equals(view);
		String type = TYPES.stream().map(String::valueOf).anyMatch(t -> t.equals(filter)) ? filter : "all";
		String seatState = STATES.contains(state) ? state : "all";
		String timelineFocus = timeline && FOCUSES.contains(focus) ? focus : "all";

		SeatVO picked = snap.seats.stream().filter(s -> s.getSeatNo().equals(selected)).findFirst().orElse(null);
		List<Map<String, Object>> seats = floorSeats(snap, type, seatState, picked);

		return row("view", timeline ? "timeline" : "floor", "waitingAvailable", snap.waitingAvailable, "filter", type,
				"state", seatState, "focus", timelineFocus, "stateOptions", STATE_OPTIONS, "selected",
				picked == null ? null : picked.getSeatNo(), "now", fmt(snap.now), // 現在時間 HH:mm（時間軸紅線上的「現在 16:20」）
				"est", EST, "seats", seats, "panel", picked == null ? null : panel(snap, picked), "closing",
				closingPreview(snap), "timeline", timeline ? timeline(snap, type, timelineFocus) : null);
	}

	/* ---------- 平面圖上的每一張桌子 ---------- */
	private List<Map<String, Object>> floorSeats(Snapshot snap, String filter, String state, SeatVO picked) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (SeatVO s : snap.seats) {
			int[] xy = LAYOUT.get(s.getSeatNo());
			boolean late = snap.isLate(s);
			boolean anomaly = snap.isAnomaly(s);
			// 桌子下面的小字：使用中的兩種狀態顏色一樣，用文字分「訂位 35 分」「現場 35 分」
			String sub = anomaly ? "時間異常"
					: snap.isSeated(s)
							? (SeatingSeatService.RESERVED.equals(s.getSeatStatus()) ? "訂位 " : "現場 ") + snap.minsOf(s)
									+ " 分"
							: s.getSeatStatus();
			String title = s.getSeatNo() + " " + s.getSeatStatus()
					+ (late ? "・用餐 " + snap.minsOf(s) + " 分鐘，超過 " + EST + " 分鐘" : "")
					+ (anomaly ? "・入座時間 " + fmt(minuteOf(s.getSeatTime())) + " 比現在還晚" : "");
			list.add(row("no", s.getSeatNo(), "type", s.getSeatTypeNo(), "status", s.getSeatStatus(), "cls",
					statusClass(s), "placed", xy != null, "x", xy == null ? 0 : xy[0], "y", xy == null ? 0 : xy[1],
					"sub", sub, "late", late, "anomaly", anomaly, "dim", isDim(snap, s, filter, state), "selected",
					picked != null && picked.getSeatNo().equals(s.getSeatNo()), "title", title));
		}
		return list;
	}

	// 變淡：桌型不符，或狀態不符（兩個篩選疊加）
	private boolean isDim(Snapshot snap, SeatVO s, String filter, String state) {
		boolean typeOk = "all".equals(filter) || String.valueOf(s.getSeatTypeNo()).equals(filter);
		return !typeOk || !matchState(snap, s, state);
	}

	private boolean matchState(Snapshot snap, SeatVO s, String state) {
		return switch (state) {
		case "free" -> SeatingSeatService.FREE.equals(s.getSeatStatus());
		case "use" -> SeatingSeatService.isInUse(s);
		case "off" -> SeatingSeatService.OFF.equals(s.getSeatStatus());
		case "late" -> snap.isLate(s);
		case "anomaly" -> snap.isAnomaly(s);
		default -> true;
		};
	}

	/* ---------- 右邊：選到的那一桌 ---------- */
	private Map<String, Object> panel(Snapshot snap, SeatVO s) {
		int type = s.getSeatTypeNo();
		Quota q = snap.quota(type);
		String mode = SeatingSeatService.isInUse(s) ? "use"
				: SeatingSeatService.OFF.equals(s.getSeatStatus()) ? "off" : "free";

		// 用餐中
		Integer mins = snap.minsOf(s);
		List<OrdersVO> current = snap.currentOrders(s);
		List<OrdersVO> active = snap.activeOrders(s);
		List<OrdersVO> unpaid = snap.unpaidOrders(s);
		int cancelled = current.size() - active.size();
		String payLabel = active.isEmpty() ? "尚未點餐" : unpaid.isEmpty() ? "已結帳" : "未結帳";
		// 例：共 2 張訂單，1 張未結帳（1 張已取消）；「共」包含已取消的單。只有 1 張單時不顯示（看「未結帳／已結帳」就夠）
		String payDetail = current.size() < 2 ? null
				: "共 " + current.size() + " 張訂單，" + unpaid.size() + " 張未結帳"
						+ (cancelled > 0 ? "（" + cancelled + " 張已取消）" : "");
		String meterLevel = mins == null ? "" : mins >= EST ? "over" : mins >= EST * 3 / 4 ? "mid" : "";

		// 空桌：可以選的訂位、候位
		List<Map<String, Object>> rsvComing = new ArrayList<>();
		List<Map<String, Object>> rsvLate = new ArrayList<>();
		List<Map<String, Object>> waitingList = new ArrayList<>();
		if ("free".equals(mode)) {
			for (ReservationVO r : snap.pendingOf(type)) {
				Map<String, Object> item = row("no", r.getRsvNo(), "time", fmt(slotOf(r)), "name",
						r.getMember() == null ? "" : r.getMember().getMemName(), "note", r.getRsvComment());
				(slotOf(r) >= snap.now ? rsvComing : rsvLate).add(item);
			}
			snap.waiting.get(type).stream().sorted(Comparator.comparingLong(WaitingTicket::sequence))
					.forEach(ticket -> waitingList.add(row("no", ticket.ticketId(), "number", ticket.getTicketDisplay(),
							"name", ticket.name(), "note", ticket.note(), "status", ticket.getStatusText())));
		}

		return row("no", s.getSeatNo(), "type", type, "status", s.getSeatStatus(), "cls", statusClass(s), "zone",
				zoneOf(s.getSeatNo()), "mode", mode,
				// 用餐中
				"seatTime", s.getSeatTime() == null ? null : fmt(minuteOf(s.getSeatTime())), "mins", mins, "anomaly",
				snap.isAnomaly(s), "meterPct", mins == null ? 0 : Math.min(mins * 100 / EST, 100), "meterLevel",
				meterLevel, "late", snap.isLate(s), "payLabel", payLabel, "payDetail", payDetail, "customer",
				"use".equals(mode) ? customerOf(active) : null, "leaveConfirm",
				unpaid.isEmpty() ? null : s.getSeatNo() + " 還有 " + unpaid.size() + " 張未結帳的訂單，確定客人要離席嗎？",
				// 空桌
				"quotaText", type + " 人桌：空桌 " + q.free() + "、需保留 " + q.need() + "、現場還能坐 " + q.walkLeft(), "walkWarn",
				q.walkLeft() == 0, "walkConfirm",
				q.walkLeft() == 0
						? type + " 人桌剩下的 " + q.free() + " 桌空桌都要保留給 90 分鐘內的 " + q.need() + " 組訂位。現場客坐下後最多佔用 " + EST
								+ " 分鐘，可能讓訂位客人沒位子。確定仍要入座？"
						: null,
				"rsvComing", rsvComing, "rsvLate", rsvLate, "waiting", waitingList, "rsvCount",
				rsvComing.size() + rsvLate.size());
	}

	/*
	 * 客人資訊：只看這桌「現在這組客人」的訂單 · 還沒有訂單 → null（畫面不顯示這一列） ·
	 * 有訂單：取第一張有會員的單（OrdersVO.getMember()，member_no 對到的會員）；都沒有 → 非會員 member 是
	 * LAZY，讀欄位時才查資料庫；member_no 對不到會員時會丟 EntityNotFoundException，一樣當作非會員
	 * 只複製要顯示的欄位，不把 MemberVO（含密碼）放進畫面
	 */
	private Map<String, Object> customerOf(List<OrdersVO> active) {
		if (active.isEmpty()) {
			return null;
		}
		MemberVO m = active.stream().map(OrdersVO::getMember).filter(mem -> mem != null).findFirst().orElse(null);
		try {
			if (m != null) {
				return row("member", true, "name", m.getMemName(), "tel", m.getMemTel(), "tags",
						splitTags(m.getMemTag()));
			}
		} catch (EntityNotFoundException e) {
			// 查不到會員：往下顯示非會員
		}
		return row("member", false, "name", "非會員", "tel", null, "tags", List.of());
	}

	/* ---------- 打烊一鍵清桌：預覽（確認視窗的文字） ---------- */
	private Map<String, Object> closingPreview(Snapshot snap) {
		List<String> clear = new ArrayList<>();
		List<String> skip = new ArrayList<>();
		for (SeatVO s : snap.seats) {
			if (!SeatingSeatService.isInUse(s)) {
				continue;
			}
			(snap.unpaidOrders(s).isEmpty() ? clear : skip).add(s.getSeatNo());
		}
		String confirm = "打烊一鍵清桌：\n清回「可使用」" + clear.size() + " 桌"
				+ (clear.isEmpty() ? "" : "（" + String.join("、", clear) + "）")
				+ (skip.isEmpty() ? "" : "\n跳過 " + skip.size() + " 桌（還有未結帳訂單）：" + String.join("、", skip))
				+ "\n停用的桌子維持停用。確定嗎？";
		return row("clear", clear, "skip", skip, "confirm", confirm);
	}

	/* ---------- 時間軸 ---------- */
	/*
	 * focus：點上方卡片後只顯示一部分（卡片上的數字永遠算全部） busy：只留用餐中的桌子，不顯示預約列 rsv ：只留預約列，而且只有 90
	 * 分鐘內、還沒到的預約（和卡片的數字一致） warn：只留超時、時間異常的桌子，預約列只留桌數不夠（紅色 !）的預約；沒有的桌型整組不顯示
	 */
	private Map<String, Object> timeline(Snapshot snap, String filter, String focus) {
		int now = snap.now;
		// 範圍：現在前後各 4 小時，左邊往前取整點、右邊往後取整點，一個畫面看完不用滑動
		// 不限營業時間：清晨或深夜打開時可能跨過 00:00（刻度用 fmt 換回 0～23 點顯示）
		// 範圍外的紀錄、訂位不畫（訂位清單、桌位卡照常可以選）
		int start = Math.floorDiv(now - RANGE, 60) * 60;
		int end = Math.floorDiv(now + RANGE + 59, 60) * 60;
		Pct pct = new Pct(start, end);

		List<Map<String, Object>> hours = new ArrayList<>();
		for (int h = start; h <= end; h += 60) {
			hours.add(row("label", fmt(h), "left", pct.of(h), "first", h == start, "last", h + 60 > end));
		}

		List<Map<String, Object>> groups = new ArrayList<>();
		int conflicts = 0;
		for (Integer type : TYPES) {
			List<SeatVO> list = snap.seatsOf(type);
			int off = (int) list.stream().filter(s -> SeatingSeatService.OFF.equals(s.getSeatStatus())).count();
			int busy = (int) list.stream().filter(snap::isSeated).count();
			Map<String, Object> lane = lane(snap, type, pct, focus);
			conflicts += (int) lane.get("conflicts");
			if (filter != null && TYPES.stream().map(String::valueOf).anyMatch(filter::equals)
					&& !filter.equals(String.valueOf(type))) {
				continue;
			}
			List<Map<String, Object>> rows = new ArrayList<>();
			for (SeatVO s : list) {
				boolean keep = switch (focus) {
				case "busy" -> snap.isSeated(s);
				case "rsv" -> false;
				case "warn" -> snap.isLate(s) || snap.isAnomaly(s);
				default -> true;
				};
				if (keep) {
					rows.add(timelineRow(snap, s, pct));
				}
			}
			boolean showLane = switch (focus) {
			case "busy" -> false;
			case "rsv", "warn" -> !((List<?>) lane.get("blocks")).isEmpty();
			default -> true;
			};
			lane.put("show", showLane);
			if (rows.isEmpty() && !showLane) {
				continue; // 這個桌型沒有符合的項目，整組不顯示
			}
			groups.add(row("type", type, "sub", list.size() + " 桌・用餐中 " + busy + (off > 0 ? "・停用 " + off : ""), "rows",
					rows, "lane", lane));
		}

		int usable = (int) snap.seats.stream().filter(s -> !SeatingSeatService.OFF.equals(s.getSeatStatus())).count();
		int busy = (int) snap.seats.stream().filter(snap::isSeated).count();
		int needTotal = TYPES.stream().mapToInt(t -> snap.quota(t).need()).sum();
		int warn = (int) snap.seats.stream().filter(s -> snap.isLate(s) || snap.isAnomaly(s)).count() + conflicts;

		return row("hours", hours, "groups", groups, "nowLeft", now >= start && now <= end ? pct.of(now) : null,
				"hourWidth", pct.of(start + 60), "busy", busy, "usable", usable, "busyPct",
				usable == 0 ? 0 : busy * 100 / usable, "ordersToday",
				snap.orders.stream().filter(o -> !isCancelled(o)).count(), "needTotal", needTotal, "warn", warn);
	}

	private Map<String, Object> timelineRow(Snapshot snap, SeatVO s, Pct pct) {
		List<Map<String, Object>> blocks = new ArrayList<>();
		List<OrdersVO> current = snap.currentOrders(s);
		Integer seatedMin = snap.isSeated(s) ? minuteOf(s.getSeatTime()) : null;

		// 稍早用過餐：不是現在這組客人的訂單（取消的不畫）。同一組客人加點的多張單（第一張起 90 分鐘內）合成一條
		List<OrdersVO> past = snap.orders.stream()
				.filter(o -> s.getSeatNo().equals(o.getSeatNo().getSeatNo()) && !current.contains(o) && !isCancelled(o))
				.filter(o -> o.getOrdersTime() != null).sorted(Comparator.comparing(OrdersVO::getOrdersTime)).toList();
		int i = 0;
		while (i < past.size()) {
			int first = minuteOf(past.get(i).getOrdersTime().toLocalTime());
			int count = 0;
			while (i < past.size() && minuteOf(past.get(i).getOrdersTime().toLocalTime()) < first + EST) {
				count++;
				i++;
			}
			int end = Math.min(first + EST, snap.now);
			if (seatedMin != null && seatedMin > first) {
				end = Math.min(end, seatedMin);
			}
			end = Math.max(end, first + 1);
			if (end <= pct.start()) {
				continue; // 整段都在時間軸左邊界之前，不畫
			}
			blocks.add(block(
					"done", "blk done", pct.of(first), pct.of(end) - pct.of(first), fmt(first), "稍早用過餐：" + fmt(first)
							+ " 下單" + (count > 1 ? "（共 " + count + " 張）" : "") + "，約 " + fmt(first + EST) + " 結束",
					0, 0));
		}

		// 現在這組客人：從入座到「入座 + 90 分鐘」和「現在」較晚的那一個；實心到現在，超時的部分需提醒是紅色、不影響是橘色
		if (seatedMin != null) {
			int end = seatedMin + EST;
			int right = Math.max(end, snap.now);
			double span = Math.max(right - seatedMin, 1);
			boolean late = snap.isLate(s);
			String pay = snap.activeOrders(s).isEmpty() ? "尚未點餐" : snap.isPaid(s) ? "已結帳" : "未結帳";
			String text = pay + "・" + snap.minsOf(s) + " 分";
			blocks.add(block("now", "blk now " + statusClass(s) + (late ? " over" : ""), pct.of(seatedMin),
					Math.max(pct.of(right) - pct.of(seatedMin), 0.8), text,
					s.getSeatNo() + " " + text + "，預計 " + fmt(end) + " 離開" + (late ? "；已超過 " + EST + " 分鐘" : ""),
					round(Math.min(snap.now - seatedMin, EST) / span * 100),
					round(snap.now > end ? (snap.now - end) / span * 100 : 0)));
		}

		String statusText;
		boolean warn = false;
		if (SeatingSeatService.OFF.equals(s.getSeatStatus())) {
			statusText = "停用";
		} else if (snap.isAnomaly(s)) {
			statusText = "時間異常";
			warn = true;
		} else if (snap.isSeated(s)) {
			statusText = snap.minsOf(s) + " 分" + (snap.isLate(s) ? "・超時" : "");
			warn = snap.isLate(s);
		} else {
			statusText = "空桌";
		}
		String note = SeatingSeatService.OFF.equals(s.getSeatStatus()) ? "停用中"
				: snap.isAnomaly(s) ? "入座時間 " + fmt(minuteOf(s.getSeatTime())) + " 比現在還晚（多半是之前沒清桌）" : null;
		return row("no", s.getSeatNo(), "cls", statusClass(s), "statusText", statusText, "warn", warn, "off",
				SeatingSeatService.OFF.equals(s.getSeatStatus()), "blocks", blocks, "note", note, "noteWarn",
				snap.isAnomaly(s));
	}

	// 每個桌型最下面一列「預約」：訂位只有桌型、沒有桌號，一筆一塊，同一個時段往下疊
	// focus＝rsv 只畫 90 分鐘內、還沒到的預約；focus＝warn 只畫桌數不夠的預約（conflicts 照樣算全部）
	private Map<String, Object> lane(Snapshot snap, int type, Pct pct, String focus) {
		List<Map<String, Object>> blocks = new ArrayList<>();
		TreeSet<Integer> slots = new TreeSet<>();
		snap.reservations.stream().filter(r -> r.getSeatType().getSeatTypeNo() == type)
				.forEach(r -> slots.add(slotOf(r)));
		int maxStack = 1;
		int conflicts = 0;
		for (int slot : slots) {
			List<ReservationVO> all = snap.reservations.stream().filter(r -> r.getSeatType().getSeatTypeNo() == type
					&& slotOf(r) == slot && !RSV_CANCELLED.equals(r.getRsvStatus())).toList();
			if (all.isEmpty()) {
				continue;
			}
			if (slot + EST <= pct.start()) {
				continue; // 整段都在時間軸左邊界之前，不畫
			}
			boolean pastSlot = slot < snap.now;
			int[] check = snap.slotCheck(type, slot);
			conflicts += check[2]; // 桌數不夠照樣算進「需要注意」，即使時段在右邊界之後
			if (slot >= pct.end()) {
				continue; // 時段在時間軸右邊界之後，不畫
			}
			boolean soon = slot >= snap.now && slot < snap.now + EST;
			if (("rsv".equals(focus) && !soon) || ("warn".equals(focus) && check[2] == 0)) {
				continue;
			}
			double left = pct.of(slot), width = pct.of(slot + EST) - pct.of(slot);
			int row = 0;
			if (pastSlot) {
				// 未到：CJ 的訂位管理（markNoShowByStaff）標記的；逾時未到：時間過了、還是「預約」
				long arrived = all.stream().filter(r -> RSV_ARRIVED.equals(r.getRsvStatus())).count();
				long marked = all.stream().filter(r -> RSV_NO_SHOW.equals(r.getRsvStatus())).count();
				long noShow = all.stream().filter(r -> RSV_PENDING.equals(r.getRsvStatus())).count();
				blocks.add(withTop(block("hold", "blk hold past", left, width,
						fmt(slot) + " 實到 " + arrived + (marked > 0 ? "・未到 " + marked : "")
								+ (noShow > 0 ? "・逾時未到 " + noShow : ""),
						fmt(slot) + " 的訂位：實到 " + arrived + " 組" + (marked > 0 ? "，未到 " + marked + " 組" : "")
								+ (noShow > 0 ? "，逾時未到 " + noShow + " 組" : ""),
						0, 0), row++));
			}
			for (ReservationVO r : all) {
				if (!RSV_PENDING.equals(r.getRsvStatus())) {
					continue;
				}
				String name = r.getMember() == null ? "" : r.getMember().getMemName();
				String title = fmt(slot) + " " + name + "（" + (r.getRsvComment() == null ? "" : r.getRsvComment()) + "）"
						+ (pastSlot ? "・逾時未到"
								: "・需要 " + check[0] + " 桌、預計空出 " + check[1] + " 桌"
										+ (check[2] == 1 ? "，桌數不夠，還差 " + (check[0] - check[1]) + " 桌" : ""));
				blocks.add(withTop(block("hold",
						"blk hold st-hold" + (pastSlot ? " past" : "") + (check[2] == 1 ? " conflict" : ""), left,
						width, fmt(slot) + " " + name + (pastSlot ? "・逾時未到" : ""), title, 0, 0), row++));
			}
			maxStack = Math.max(maxStack, row);
		}
		Quota q = snap.quota(type);
		return row("sub", "需保留 " + q.need() + (q.noShow() > 0 ? "・逾時未到 " + q.noShow() : ""), "height",
				16 + maxStack * 38, "blocks", blocks, "conflicts", conflicts);
	}

	private Map<String, Object> block(String kind, String cls, double left, double width, String text, String title,
			double fill, double over) {
		return row("kind", kind, "cls", cls, "left", round(left), "width", round(width), "top", 8, "text", text,
				"title", title, "fill", fill, "over", over);
	}

	private Map<String, Object> withTop(Map<String, Object> block, int stackRow) {
		block.put("top", 8 + stackRow * 38);
		return block;
	}

	// 時間 → 時間軸上的百分比位置
	private record Pct(int start, int end) {
		double of(int minute) {
			double p = (minute - start) * 100.0 / (end - start);
			return round(Math.max(0, Math.min(100, p)));
		}
	}

	/*
	 * =====================================================================
	 * 三、操作（Controller 的 POST 呼叫；失敗丟 IllegalArgumentException／IllegalStateException）
	 * 組員方法丟出的錯誤不在交易裡 catch，交給 Controller 顯示訊息
	 * =====================================================================
	 */

	@Transactional
	public void seatWalkIn(String seatNo) {
		seatService.seatIn(seatNo, SeatingSeatService.WALK_IN);
	}

	/*
	 * 訂位者入座：桌子 → 訂位者使用；訂位 → 實到（先鎖桌子再鎖訂位，順序固定，避免互卡） 訂位改實到用 CJ 的
	 * ReservationService.markArrivedByStaff：它會鎖住訂位，檢查還是「預約」、而且是今天的訂位
	 */
	@Transactional
	public void seatReservation(String seatNo, Long rsvNo) {
		if (rsvNo == null) {
			throw new IllegalArgumentException("請選擇一筆訂位");
		}
		SeatVO seat = seatService.seatIn(seatNo, SeatingSeatService.RESERVED);
		Integer rsvType = reservationService.getOneReservation(rsvNo).getSeatType().getSeatTypeNo();
		if (!rsvType.equals(seat.getSeatTypeNo())) {
			throw new IllegalArgumentException("訂位 #" + rsvNo + " 是 " + rsvType + " 人桌，桌型不符");
		}
		reservationService.markArrivedByStaff(rsvNo);
	}

	// 候位入座：桌子 → 其它者使用；Redis 號碼牌 → 已入座
	// 先改桌子再改號碼牌：號碼牌已入座／過號／取消，或不是這個桌型（Redis 依桌型分開存，會找不到）都會丟錯誤，
	// 交易還原，桌子不會變成使用中；兩個人同時按同一組，Redis 的 Lua 只會讓一個人成功
	// 舊的 MySQL 候位入座入口停止使用。
	// 實際分桌待與 Redis 候位完成一致性串接後啟用。
	public void seatWaiting(String seatNo, String ticketId) {
		if (ticketId == null || ticketId.isBlank()) {
			throw new IllegalArgumentException("請選擇一組候位");
		}
		SeatVO seat = seatService.seatIn(seatNo, SeatingSeatService.WALK_IN);
		waitingRedisService.seatWaiting(seat.getSeatTypeNo(), ticketId);
	}

	@Transactional
	public void leave(String seatNo) {
		seatService.leave(seatNo);
	}

	@Transactional
	public void disable(String seatNo) {
		seatService.disable(seatNo);
	}

	@Transactional
	public void enable(String seatNo) {
		seatService.enable(seatNo);
	}

	/*
	 * 結帳＝空桌：現在這組客人有訂單、而且全部結帳了，桌子就清回「可使用」，店員不用按「客人離席」 結帳是組員的
	 * OrdersService.pay（不改它），所以由入座管理在每次打開頁面時檢查（Controller 的 GET 先呼叫這裡）； 頁面每 60
	 * 秒會自動重新整理，結帳後最慢 1 分鐘桌子就會變空桌
	 */
	@Transactional
	public void releasePaidSeats() {
		Snapshot snap = new Snapshot();
		for (SeatVO s : snap.seats) {
			if (SeatingSeatService.isInUse(s) && snap.isPaid(s)) {
				seatService.release(s.getSeatNo());
			}
		}
	}

	/*
	 * 打烊一鍵清桌：使用中的桌子清回「可使用」；還有未結帳訂單的跳過，停用的不動 不改訂位（逾時未到只在畫面上顯示），也不改候位（交給候位負責的組員） 回傳
	 * [清回幾桌, 跳過幾桌]
	 */
	@Transactional
	public int[] closing() {
		Snapshot snap = new Snapshot();
		int cleared = 0, skipped = 0;
		for (SeatVO s : snap.seats) {
			if (!SeatingSeatService.isInUse(s)) {
				continue;
			}
			if (!snap.unpaidOrders(s).isEmpty()) {
				skipped++;
				continue;
			}
			seatService.release(s.getSeatNo());
			cleared++;
		}
		return new int[] { cleared, skipped };
	}

	/*
	 * ===================================================================== 四、小工具
	 * =====================================================================
	 */

	// 訂單狀態照 G1：明細全部「已取消」= CANCELLED；ordersPay 有值 = 已結帳，null 或空字串 = 未結帳
	private static boolean isCancelled(OrdersVO o) {
		return o.getOrdersDetails() != null && "CANCELLED".equals(o.getStatus());
	}

	private static boolean isUnpaid(OrdersVO o) {
		return o.getOrdersPay() == null || o.getOrdersPay().isBlank();
	}

	private static int slotOf(ReservationVO r) {
		return minuteOf(r.getRsvDt().getRsvDtDatetime().toLocalTime());
	}

	private static int minuteOf(LocalTime t) {
		return t.getHour() * 60 + t.getMinute();
	}

	// 分鐘 → HH:mm；超過 24:00 或小於 00:00（時間軸跨日、入座 + 90 分鐘超過午夜）換回 0～23 點
	// 會員的客戶備註（member.mem_tag）用「；」「;」「、」或換行分成幾點，畫面上條列顯示
	private static List<String> splitTags(String tag) {
		if (tag == null || tag.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(tag.split("[；;、\n]")).map(String::trim).filter(t -> !t.isEmpty()).toList();
	}

	private static String fmt(int minute) {
		int m = Math.floorMod(minute, 24 * 60);
		return String.format("%02d:%02d", m / 60, m % 60);
	}

	private static double round(double v) {
		return Math.round(v * 100) / 100.0;
	}

	// 訂位者使用、其它者使用同一個顏色（st-use）；平面圖、桌位卡、時間軸都用這個
	private static String statusClass(SeatVO s) {
		return switch (s.getSeatStatus() == null ? "" : s.getSeatStatus()) {
		case SeatingSeatService.RESERVED, SeatingSeatService.WALK_IN -> "st-use";
		case SeatingSeatService.OFF -> "st-off";
		default -> "st-free";
		};
	}

	private static String zoneOf(String seatNo) {
		int[] xy = LAYOUT.get(seatNo);
		if (xy == null) {
			return "其他";
		}
		return xy[0] < 32 ? "窗邊" : xy[0] < 75 ? "中央" : "包廂";
	}

	// 用 key, value, key, value… 建一個 Map（值可以是 null，Map.of 不行）
	private static Map<String, Object> row(Object... kv) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			map.put((String) kv[i], kv[i + 1]);
		}
		return map;
	}

	// 沿用入座管理的計算，不另外維護另一份桌數。
	public Map<Integer, Integer> getWaitingAvailableTables() {
		Snapshot snapshot = new Snapshot();

		Map<Integer, Integer> result = new LinkedHashMap<>();

		for (Integer type : TYPES) {
			result.put(type, snapshot.quota(type).walkLeft());
		}

		return result;
	}
}
