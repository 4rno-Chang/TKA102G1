package com.bistroops.reservation.model;

import java.time.LocalDate;
import com.bistroops.member.model.MemberService;
import com.bistroops.reservationdatetime.model.ReservationSlotWindowService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.member.model.MemberRepository;
import com.bistroops.member.model.MemberVO;
import com.bistroops.reservationdatetime.model.ReservationDatetimeRepository;
import com.bistroops.reservationdatetime.model.ReservationDatetimeVO;
import com.bistroops.seattype.model.SeatTypeRepository;
import com.bistroops.seattype.model.SeatTypeVO;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReservationService {

	private final ReservationRepository repository;
	private final MemberRepository memberRepository;
	private final ReservationDatetimeRepository rsvDtRepository;
	private final SeatTypeRepository seatTypeRepository;
	private final ReservationSlotWindowService slotWindowService;
	private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Taipei");
	private final MemberService memberService;

	public ReservationService(ReservationRepository repository, MemberRepository memberRepository,
			ReservationDatetimeRepository rsvDtRepository, SeatTypeRepository seatTypeRepository,
			ReservationSlotWindowService slotWindowService, MemberService memberService) {

		this.repository = repository;
		this.memberRepository = memberRepository;
		this.rsvDtRepository = rsvDtRepository;
		this.seatTypeRepository = seatTypeRepository;
		this.slotWindowService = slotWindowService;
		this.memberService = memberService;
	}

	public List<ReservationVO> getAll() {
		return repository.findAll();
	}

	public List<ReservationVO> search(LocalDate date, String status) {
		LocalDateTime startTime = null;
		LocalDateTime endTime = null;

		if (date != null) {
			startTime = date.atStartOfDay();
			endTime = date.plusDays(1).atStartOfDay();
		}

		String normalizedStatus = status == null || status.isBlank() ? null : status.trim();

		return repository.search(startTime, endTime, normalizedStatus);
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ReservationVO addForMember(Integer memNo, LocalDate date, Long rsvDtNo, Integer seatTypeNo,
			String rsvComment) {

		if (memNo == null) {
			throw new IllegalArgumentException("請先登入會員");
		}

		if (date == null || rsvDtNo == null) {
			throw new IllegalArgumentException("請選擇日期與時段");
		}

		LocalDate today = LocalDate.now(RESERVATION_ZONE);

		if (date.isBefore(today) || date.isAfter(today.plusDays(89))) {

			throw new IllegalArgumentException("只能預約含今日起算90天內的日期");
		}

		if (seatTypeNo == null || !Set.of(2, 4, 6).contains(seatTypeNo)) {
			throw new IllegalArgumentException("請選擇2、4或6人桌");
		}

		String comment = rsvComment == null ? "" : rsvComment.trim();

		if (comment.codePointCount(0, comment.length()) > 50) {
			throw new IllegalArgumentException("備註最多50個字元");
		}

		// 先鎖定時段，再查詢訂位紀錄
		ReservationDatetimeVO timeSlot = rsvDtRepository.findByIdForUpdate(rsvDtNo)
				.orElseThrow(() -> new IllegalArgumentException("訂位時段不存在"));

		LocalDateTime diningTime = timeSlot.getRsvDtDatetime();

		if (diningTime == null || !diningTime.toLocalDate().equals(date)) {
			throw new IllegalArgumentException("訂位時段與所選日期不符");
		}

		Set<LocalTime> allowedTimes = Set.of(LocalTime.of(11, 0), LocalTime.of(12, 30), LocalTime.of(17, 0),
				LocalTime.of(18, 30));

		if (!allowedTimes.contains(diningTime.toLocalTime())) {
			throw new IllegalArgumentException("此時段未開放訂位");
		}

		if (!diningTime.isAfter(LocalDateTime.now(RESERVATION_ZONE))) {
			throw new IllegalArgumentException("此時段已開始，請選擇其他時段");
		}

		MemberVO member = memberRepository.findById(memNo).orElseThrow(() -> new IllegalArgumentException("會員不存在"));

		SeatTypeVO seatType = seatTypeRepository.findById(seatTypeNo)
				.orElseThrow(() -> new IllegalArgumentException("桌型不存在"));

		long duplicateCount = repository.countActiveByMemberAndTime(memNo, rsvDtNo);

		if (duplicateCount > 0) {
			throw new IllegalArgumentException("你在這個時段已有訂位，如需加桌請電話聯絡餐廳");
		}

		Integer onlineLimit = seatType.getSeatTypeRsvNum();
		Integer totalTables = seatType.getSeatTypeNum();

		if (onlineLimit == null || totalTables == null || onlineLimit <= 0 || totalTables <= 0) {
			throw new IllegalArgumentException("此桌型目前未開放網頁訂位");
		}

		long usedTables = repository.countActiveByTimeAndSeatType(rsvDtNo, seatTypeNo);

		int capacity = Math.min(onlineLimit, totalTables);

		if (usedTables >= capacity) {
			throw new IllegalArgumentException("此時段的所選桌型已額滿，請改選或電話聯絡餐廳");
		}

		ReservationVO reservation = new ReservationVO();
		reservation.setMember(member);
		reservation.setRsvDt(timeSlot);
		reservation.setSeatType(seatType);
		reservation.setRsvCreateTime(LocalDateTime.now(RESERVATION_ZONE));
		reservation.setRsvStatus("預約");
		reservation.setRsvComment(comment);

		return repository.save(reservation);
	}

	public List<Map<String, Object>> getSlots(LocalDate date, Integer seatTypeNo) {

		if (date == null) {
			return List.of();
		}

		if (seatTypeNo == null || !Set.of(2, 4, 6).contains(seatTypeNo)) {
			throw new IllegalArgumentException("請選擇2、4或6人桌");
		}

		SeatTypeVO seatType = seatTypeRepository.findById(seatTypeNo)
				.orElseThrow(() -> new IllegalArgumentException("桌型不存在"));

		Integer onlineLimit = seatType.getSeatTypeRsvNum();
		Integer totalTables = seatType.getSeatTypeNum();

		int capacity = onlineLimit == null || totalTables == null ? 0 : Math.max(0, Math.min(onlineLimit, totalTables));

		Set<LocalTime> allowedTimes = Set.of(LocalTime.of(11, 0), LocalTime.of(12, 30), LocalTime.of(17, 0),
				LocalTime.of(18, 30));

		LocalDateTime now = LocalDateTime.now(RESERVATION_ZONE);

		return rsvDtRepository
				.findByRsvDtDatetimeGreaterThanEqualAndRsvDtDatetimeLessThanOrderByRsvDtDatetimeAsc(date.atStartOfDay(),
						date.plusDays(1).atStartOfDay())
				.stream().filter(slot -> slot.getRsvDtDatetime() != null)
				.filter(slot -> slot.getRsvDtDatetime().isAfter(now))
				.filter(slot -> allowedTimes.contains(slot.getRsvDtDatetime().toLocalTime())).map(slot -> {
					long used = repository.countActiveByTimeAndSeatType(slot.getRsvDtNo(), seatTypeNo);

					long remaining = Math.max(0L, capacity - used);

					return Map.<String, Object>of("rsvDtNo", slot.getRsvDtNo(), "timeText",
							slot.getRsvDtDatetime().toLocalTime().toString(), "remaining", remaining, "full",
							remaining == 0);
				}).toList();
	}

	public Map<String, Object> getResultForMember(Long rsvNo, Integer memNo) {

		ReservationVO reservation = repository.findById(rsvNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到訂位紀錄"));

		MemberVO member = reservation.getMember();

		if (!member.getMemNo().equals(memNo)) {
			throw new IllegalArgumentException("無法查看這筆訂位");
		}

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		return Map.<String, Object>of("reservationNo", reservation.getRsvNo(), "dateText",
				diningTime.toLocalDate().toString().replace('-', '/'), "tableType",
				reservation.getSeatType().getSeatTypeNo(), "timeText", diningTime.toLocalTime().toString(), "name",
				member.getMemName() == null ? "" : member.getMemName(), "phone",
				member.getMemTel() == null ? "" : member.getMemTel(), "email",
				member.getMemMail() == null ? "" : member.getMemMail(), "note",
				reservation.getRsvComment() == null ? "" : reservation.getRsvComment());
	}

	public List<ReservationVO> getByMemberNo(Integer memNo) {
		return repository.findByMemberNo(memNo);
	}

	// 會員取消自己的訂位。
	// 保留資料，只將狀態改為「取消」。
	@Transactional
	public void cancelForMember(Long rsvNo, Integer memNo) {

		ReservationVO reservation = getOwnedReservationForUpdate(rsvNo, memNo);

		requireFutureReservation(reservation);

		reservation.setRsvStatus("取消");
	}

	// 後台修改訂位備註。
	// 此方法保留原本名稱，既有 Controller 可繼續使用。
	@Transactional
	public void updateComment(Long rsvNo, String rsvComment) {

		ReservationVO reservation = getReservationForUpdate(rsvNo);

		if (!Set.of("預約", "實到").contains(reservation.getRsvStatus())) {
			throw new IllegalArgumentException("只有預約或實到的訂位可以修改備註");
		}

		reservation.setRsvComment(normalizeComment(rsvComment));
	}

	// 會員查詢自己的一筆訂位明細。
	public ReservationVO getOneForMember(Long rsvNo, Integer memNo) {

		checkMemberNo(memNo);
		checkReservationNo(rsvNo);

		ReservationVO reservation = repository.findById(rsvNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到訂位紀錄"));

		requireOwner(reservation, memNo);

		return reservation;
	}

	// 會員修改自己的訂位備註。
	// 日期、時段與桌型維持原訂位內容。
	@Transactional
	public void updateCommentForMember(Long rsvNo, Integer memNo, String rsvComment) {

		ReservationVO reservation = getOwnedReservationForUpdate(rsvNo, memNo);

		requireFutureReservation(reservation);

		reservation.setRsvComment(normalizeComment(rsvComment));
	}

	// 後台查詢一筆訂位明細。
	public ReservationVO getOneReservation(Long rsvNo) {

		checkReservationNo(rsvNo);

		return repository.findById(rsvNo).orElseThrow(() -> new IllegalArgumentException("找不到訂位紀錄"));
	}

	// 後台取消尚未結束的訂位。
	// 與會員取消不同，員工可處理已到用餐時間的預約。
	@Transactional
	public void cancelByStaff(Long rsvNo) {

		ReservationVO reservation = getReservationForUpdate(rsvNo);

		requireReserved(reservation);

		reservation.setRsvStatus("取消");
	}

	// 後台將訂位標記為「實到」。
	// 這裡只更新到店狀態，不分配桌號、不建立訂單。
	@Transactional
	public void markArrivedByStaff(Long rsvNo) {

		ReservationVO reservation = getReservationForUpdate(rsvNo);

		requireReserved(reservation);

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		if (diningTime == null) {
			throw new IllegalArgumentException("訂位缺少用餐時間");
		}

		if (!diningTime.toLocalDate().equals(LocalDate.now(RESERVATION_ZONE))) {
			throw new IllegalArgumentException("只能將今日的訂位標記為實到");
		}

		reservation.setRsvStatus("實到");
	}

	// 後台將已到用餐時間的預約標記為「未到」。
	@Transactional
	public void markNoShowByStaff(Long rsvNo) {

		ReservationVO reservation = getReservationForUpdate(rsvNo);

		requireReserved(reservation);

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		if (diningTime == null) {
			throw new IllegalArgumentException("訂位缺少用餐時間");
		}

		if (diningTime.isAfter(LocalDateTime.now(RESERVATION_ZONE))) {
			throw new IllegalArgumentException("尚未到用餐時間，不能標記為未到");
		}

		reservation.setRsvStatus("未到");
	}

	// 會員中心使用的訂位清單。
	// current：尚未到用餐時間且仍為「預約」。
	// history：其餘紀錄，包括取消、實到、未到與已過時間的預約。
	public Map<String, List<ReservationVO>> getMemberReservationGroups(Integer memNo) {

		checkMemberNo(memNo);

		List<ReservationVO> reservations = repository.findByMemberNo(memNo);

		LocalDateTime now = LocalDateTime.now(RESERVATION_ZONE);

		List<ReservationVO> current = reservations.stream()
				.filter(reservation -> isCurrentReservation(reservation, now)).toList();

		List<ReservationVO> history = reservations.stream()
				.filter(reservation -> !isCurrentReservation(reservation, now)).toList();

		return Map.of("current", current, "history", history);
	}

	// 後台首頁的訂位摘要。
	// 「最近一輪」定義為今日尚未開始、仍有預約的最近時段。
	// 不包含候位統計。
	public Map<String, Object> getStaffReservationSummary() {

		LocalDateTime now = LocalDateTime.now(RESERVATION_ZONE);
		LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();

		List<ReservationVO> upcoming = repository.findUpcomingReservations(now, tomorrow);

		Map<String, Object> summary = new LinkedHashMap<>();

		if (upcoming.isEmpty()) {
			summary.put("nextRoundTime", "今日無後續訂位");
			summary.put("nextRoundCount", 0L);

			return summary;
		}

		LocalDateTime nextTime = upcoming.get(0).getRsvDt().getRsvDtDatetime();

		long nextRoundCount = upcoming.stream()
				.filter(reservation -> nextTime.equals(reservation.getRsvDt().getRsvDtDatetime())).count();

		summary.put("nextRoundTime", nextTime.toLocalTime().toString());

		summary.put("nextRoundCount", nextRoundCount);

		return summary;
	}

	// 以下為共用檢查方法。

	private ReservationVO getReservationForUpdate(Long rsvNo) {

		checkReservationNo(rsvNo);

		return repository.findByIdForUpdate(rsvNo).orElseThrow(() -> new IllegalArgumentException("找不到訂位紀錄"));
	}

	private ReservationVO getOwnedReservationForUpdate(Long rsvNo, Integer memNo) {

		checkMemberNo(memNo);

		ReservationVO reservation = getReservationForUpdate(rsvNo);

		requireOwner(reservation, memNo);

		return reservation;
	}

	private void requireOwner(ReservationVO reservation, Integer memNo) {

		if (reservation.getMember() == null || !memNo.equals(reservation.getMember().getMemNo())) {
			throw new IllegalArgumentException("你無法操作這筆訂位");
		}
	}

	private void requireReserved(ReservationVO reservation) {

		if (!"預約".equals(reservation.getRsvStatus())) {
			throw new IllegalArgumentException("只有預約中的訂位可以執行此操作");
		}
	}

	private void requireFutureReservation(ReservationVO reservation) {

		requireReserved(reservation);

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		if (diningTime == null || !diningTime.isAfter(LocalDateTime.now(RESERVATION_ZONE))) {
			throw new IllegalArgumentException("已到用餐時間，請聯絡餐廳處理");
		}
	}

	private boolean isCurrentReservation(ReservationVO reservation, LocalDateTime now) {

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		return "預約".equals(reservation.getRsvStatus()) && diningTime != null && diningTime.isAfter(now);
	}

	private String normalizeComment(String rsvComment) {

		String comment = rsvComment == null ? "" : rsvComment.trim();

		if (comment.codePointCount(0, comment.length()) > 50) {
			throw new IllegalArgumentException("備註最多50個字元");
		}

		return comment;
	}

	private void checkReservationNo(Long rsvNo) {

		if (rsvNo == null || rsvNo <= 0) {
			throw new IllegalArgumentException("訂位編號必須大於零");
		}
	}

	private void checkMemberNo(Integer memNo) {

		if (memNo == null || memNo <= 0) {
			throw new IllegalArgumentException("請先登入有效會員");
		}
	}

	// 員工代會員新增訂位。
	// 會員以電話查找，不要求客人在前台登入。
	// 員工代客訂位，也可使用尚未建立會員的電話。
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ReservationVO addByStaff(String phone, LocalDate date, Long rsvDtNo, Integer seatTypeNo, String comment) {

		return addByPhone(phone, date, rsvDtNo, seatTypeNo, comment);
	}

	// 開啟訂位頁時，補齊含今日90天的時段，
	// 再一次查詢各日期、桌型與時段的剩餘桌數。
	// 一般新增訂位：所有訂位都計入已占用桌數。
	public Map<String, Object> getAvailabilitySnapshot() {
		return getAvailabilitySnapshot(null);
	}

	// 編輯訂位：排除目前正在編輯的這筆訂位。
	public Map<String, Object> getAvailabilitySnapshot(Long excludedReservationNo) {

		LocalDateTime now = LocalDateTime.now(RESERVATION_ZONE);

		LocalDate minDate = now.toLocalDate();
		LocalDate maxDate = minDate.plusDays(89);

		// 先補齊缺少的訂位時段。
		slotWindowService.ensureWindow(minDate);

		// 查詢包含最後一天，使用隔天00:00作為排除上限。
		LocalDateTime endExclusive = maxDate.plusDays(1).atStartOfDay();

		List<ReservationAvailabilityRow> rows = repository.findAvailability(now, endExclusive, excludedReservationNo);

		Map<String, Map<String, List<Map<String, Object>>>> slotsByDate = new LinkedHashMap<>();

		for (ReservationAvailabilityRow row : rows) {

			LocalDateTime diningTime = row.getDiningTime();

			String date = diningTime.toLocalDate().toString();

			String tableType = row.getTableType().toString();

			Map<String, List<Map<String, Object>>> tables = slotsByDate.computeIfAbsent(date,
					ignored -> new LinkedHashMap<>());

			List<Map<String, Object>> slots = tables.computeIfAbsent(tableType, ignored -> new ArrayList<>());

			long remaining = row.getRemaining() == null ? 0L : Math.max(0L, row.getRemaining());

			slots.add(Map.<String, Object>of("rsvDtNo", row.getRsvDtNo().toString(),

					"timeText", diningTime.format(DateTimeFormatter.ofPattern("HH:mm")),

					"startAt", diningTime.atZone(RESERVATION_ZONE).toInstant().toEpochMilli(),

					"remaining", remaining,

					"full", remaining == 0));
		}

		return Map.<String, Object>of("minDate", minDate.toString(), "maxDate", maxDate.toString(),

				"serverTime", now.atZone(RESERVATION_ZONE).toInstant().toEpochMilli(),

				"slotsByDate", slotsByDate);
	}

	// 免登入訂位：以電話取得或建立未驗證會員。
	// 不會自動登入，也不會修改既有會員資料。
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ReservationVO addByPhone(String phone, LocalDate date, Long rsvDtNo, Integer seatTypeNo, String comment) {

		String normalizedPhone = normalizeBookingPhone(phone);

		if (date == null || rsvDtNo == null) {
			throw new IllegalArgumentException("請選擇日期與時段");
		}

		LocalDate today = LocalDate.now(RESERVATION_ZONE);

		if (date.isBefore(today) || date.isAfter(today.plusDays(89))) {

			throw new IllegalArgumentException("只能預約含今日起算90天內的日期");
		}

		if (seatTypeNo == null || !Set.of(2, 4, 6).contains(seatTypeNo)) {

			throw new IllegalArgumentException("請選擇2、4或6人桌");
		}

		String normalizedComment = normalizeComment(comment);

		MemberVO member = memberService.getOrCreateUnverifiedMemberForReservation(normalizedPhone);

		// 沿用已完成的時段鎖定、日期核對、
		// 重複訂位及剩餘桌數檢查。
		return addForMember(member.getMemNo(), date, rsvDtNo, seatTypeNo, normalizedComment);
	}

	// 前後台共用電話格式檢查。
	private String normalizeBookingPhone(String phone) {

		String value = phone == null ? "" : phone.trim();

		if (!value.matches("09[0-9]{8}")) {
			throw new IllegalArgumentException("請輸入09開頭的10碼手機號碼");
		}

		return value;
	}

	// 公開電話查詢只提供必要訂位資訊。
	// 不包含會員姓名、Email、備註或密碼。
	public List<Map<String, Object>> lookupByPhone(String phone) {

		String normalizedPhone = normalizeBookingPhone(phone);

		LocalDateTime fromTime = LocalDate.now(RESERVATION_ZONE).atStartOfDay();

		return repository.findUpcomingByPhone(normalizedPhone, fromTime).stream().map(reservation -> {

			LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

			return Map.<String, Object>of("reservationNo", reservation.getRsvNo(),

					"dateText", diningTime.toLocalDate().toString().replace('-', '/'),

					"timeText", diningTime.format(DateTimeFormatter.ofPattern("HH:mm")),

					"tableType", reservation.getSeatType().getSeatTypeNo(),

					"status", reservation.getRsvStatus() == null ? "未記錄" : reservation.getRsvStatus());
		}).toList();
	}

	// 僅供已確認本瀏覽器訂位權限的 Controller 呼叫。
	// 不回傳會員姓名、Email 或密碼。
	public Map<String, Object> getResultForBooking(Long rsvNo) {

		checkReservationNo(rsvNo);

		ReservationVO reservation = repository.findById(rsvNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到訂位紀錄"));

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		return Map.<String, Object>of("reservationNo", reservation.getRsvNo(),

				"dateText", diningTime.toLocalDate().toString().replace('-', '/'),

				"timeText", diningTime.format(DateTimeFormatter.ofPattern("HH:mm")),

				"tableType", reservation.getSeatType().getSeatTypeNo(),

				"bookingName", reservation.getRsvName() == null ? "" : reservation.getRsvName(),

				"phone", reservation.getMember().getMemTel(),

				"note", reservation.getRsvComment() == null ? "" : reservation.getRsvComment(),

				"status", reservation.getRsvStatus() == null ? "未記錄" : reservation.getRsvStatus());
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ReservationVO addByPhone(String phone, String bookingName, LocalDate date, Long rsvDtNo, Integer seatTypeNo,
			String comment) {

		String normalizedName = bookingName == null ? "" : bookingName.trim();

		if (normalizedName.codePointCount(0, normalizedName.length()) > 30) {
			throw new IllegalArgumentException("聯絡姓名最多30個字元");
		}

		// 沿用原本的電話、日期、桌數及重複訂位檢查。
		ReservationVO reservation = addByPhone(phone, date, rsvDtNo, seatTypeNo, comment);

		// 此處只修改訂位資料，不修改會員資料。
		reservation.setRsvName(normalizedName.isEmpty() ? null : normalizedName);

		return repository.save(reservation);
	}

	public Map<String, List<Map<String, Object>>> getMemberReservationSnapshot(Integer memNo) {

		Map<String, List<ReservationVO>> groups = getMemberReservationGroups(memNo);

		return Map.of("current", groups.get("current").stream().map(this::toMemberReservationData).toList(),

				"history", groups.get("history").stream().map(this::toMemberReservationData).toList());
	}

	private Map<String, Object> toMemberReservationData(ReservationVO reservation) {

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		Map<String, Object> result = new LinkedHashMap<>();

		result.put("reservationNo", reservation.getRsvNo());

		result.put("dateText", diningTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));

		result.put("timeText", diningTime.format(DateTimeFormatter.ofPattern("HH:mm")));

		result.put("tableType", reservation.getSeatType().getSeatTypeNo());

		result.put("bookingName", reservation.getRsvName() == null ? "" : reservation.getRsvName());

		result.put("status", reservation.getRsvStatus() == null ? "" : reservation.getRsvStatus());

		result.put("note", reservation.getRsvComment() == null ? "" : reservation.getRsvComment());

		return result;
	}

	// 編輯訂位：保留原編號，在同一個交易內更新日期、桌型與聯絡資料。
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void editForMember(Long rsvNo, Integer memNo, LocalDate date, Long rsvDtNo, Integer tableType,
			String bookingName, String note) {

		MemberVO member = memberRepository.findById(memNo).orElseThrow(() -> new IllegalArgumentException("會員不存在"));

		if (!"已驗證".equals(member.getMemStatus())) {
			throw new IllegalArgumentException("請先完成電話驗證");
		}

		ReservationVO reservation = getOwnedReservationForUpdate(rsvNo, memNo);

		applyReservationEdit(reservation, date, rsvDtNo, tableType, bookingName, note);
	}

	// 員工編輯不要求該訂位會員已驗證。
	// 員工登入身分由後台 Controller 檢查。
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void editByStaff(Long rsvNo, LocalDate date, Long rsvDtNo, Integer tableType, String bookingName,
			String note) {

		ReservationVO reservation = getReservationForUpdate(rsvNo);

		applyReservationEdit(reservation, date, rsvDtNo, tableType, bookingName, note);
	}

	// 沿用既有的時間、容量、重複訂位及鎖定檢查。
	private void applyReservationEdit(ReservationVO reservation, LocalDate date, Long rsvDtNo, Integer tableType,
			String bookingName, String note) {

		Integer memNo = reservation.getMember().getMemNo();

		requireFutureReservation(reservation);

		LocalDateTime now = LocalDateTime.now(RESERVATION_ZONE);
		LocalDate today = now.toLocalDate();

		if (date == null || date.isBefore(today) || date.isAfter(today.plusDays(89))) {
			throw new IllegalArgumentException("只能預約含今日起算90天內的日期");
		}

		if (rsvDtNo == null) {
			throw new IllegalArgumentException("請選擇用餐時段");
		}

		if (tableType == null || !Set.of(2, 4, 6).contains(tableType)) {
			throw new IllegalArgumentException("請選擇2、4或6人桌");
		}

		String name = bookingName == null ? "" : bookingName.trim();

		if (name.codePointCount(0, name.length()) > 30) {
			throw new IllegalArgumentException("聯絡姓名最多30個字元");
		}

		String comment = normalizeComment(note);

		Long originalSlotNo = reservation.getRsvDt().getRsvDtNo();
		Integer originalTableType = reservation.getSeatType().getSeatTypeNo();

		// 依固定順序鎖定原時段及新時段。
		Long firstSlotNo = Math.min(originalSlotNo, rsvDtNo);
		Long secondSlotNo = Math.max(originalSlotNo, rsvDtNo);

		ReservationDatetimeVO firstSlot = rsvDtRepository.findByIdForUpdate(firstSlotNo)
				.orElseThrow(() -> new IllegalArgumentException("訂位時段不存在"));

		ReservationDatetimeVO secondSlot = firstSlot;

		if (!firstSlotNo.equals(secondSlotNo)) {
			secondSlot = rsvDtRepository.findByIdForUpdate(secondSlotNo)
					.orElseThrow(() -> new IllegalArgumentException("訂位時段不存在"));
		}

		ReservationDatetimeVO targetSlot = rsvDtNo.equals(firstSlotNo) ? firstSlot : secondSlot;

		LocalDateTime targetTime = targetSlot.getRsvDtDatetime();

		if (targetTime == null || !date.equals(targetTime.toLocalDate())) {
			throw new IllegalArgumentException("日期與時段不一致");
		}

		Set<LocalTime> allowedTimes = Set.of(LocalTime.of(11, 0), LocalTime.of(12, 30), LocalTime.of(17, 0),
				LocalTime.of(18, 30));

		if (!allowedTimes.contains(targetTime.toLocalTime())) {
			throw new IllegalArgumentException("此時段未開放訂位");
		}

		// 再次確認時間，避免等待鎖定期間已到用餐時間。
		now = LocalDateTime.now(RESERVATION_ZONE);

		if (!reservation.getRsvDt().getRsvDtDatetime().isAfter(now) || !targetTime.isAfter(now)) {
			throw new IllegalArgumentException("已到用餐時間，請聯絡餐廳處理");
		}

		SeatTypeVO targetTable = seatTypeRepository.findById(tableType)
				.orElseThrow(() -> new IllegalArgumentException("桌型不存在"));

		Integer onlineLimit = targetTable.getSeatTypeRsvNum();
		Integer physicalLimit = targetTable.getSeatTypeNum();

		if (onlineLimit == null || physicalLimit == null || onlineLimit <= 0 || physicalLimit <= 0) {
			throw new IllegalArgumentException("此桌型未開放網路訂位");
		}

		// 排除正在編輯的這筆訂位，檢查同會員同時段重複訂位。
		long duplicates = repository.countActiveByMemberAndTime(memNo, rsvDtNo);

		if (originalSlotNo.equals(rsvDtNo)) {
			duplicates--;
		}

		if (duplicates > 0) {
			throw new IllegalArgumentException("你在這個時段已有其他訂位");
		}

		long usedTables = repository.countActiveByTimeAndSeatType(rsvDtNo, tableType);

		// 原時段、原桌型本身占用的一桌不計入競爭名額。
		if (originalSlotNo.equals(rsvDtNo) && originalTableType.equals(tableType)) {
			usedTables--;
		}

		int capacity = Math.min(onlineLimit, physicalLimit);

		if (usedTables >= capacity) {
			throw new IllegalArgumentException("所選時段已額滿，原訂位仍保留，請選擇其他時段");
		}

		reservation.setRsvDt(targetSlot);
		reservation.setSeatType(targetTable);
		reservation.setRsvName(name.isEmpty() ? null : name);
		reservation.setRsvComment(comment);

		repository.save(reservation);
	}
	
	// 每日訂位提醒使用，只取得需要的資料。
	public List<Map<String, Object>> getDailyReminderTargets(LocalDate date) {

	    return search(date, "預約").stream()
	            .map(reservation -> {
	                var diningTime =
	                        reservation.getRsvDt().getRsvDtDatetime();

	                String phone = reservation.getMember().getMemTel();

	                return Map.<String, Object>of(
	                        "reservationNo", reservation.getRsvNo(),
	                        "phone", phone == null ? "" : phone,
	                        "timeText", diningTime.format(
	                                DateTimeFormatter.ofPattern("HH:mm")),
	                        "tableType",
	                        reservation.getSeatType().getSeatTypeNo());
	            })
	            .toList();
	}

}
