package com.bistroops.reservation.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

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

@Service
@Transactional(readOnly = true)
public class ReservationService {

	private final ReservationRepository repository;
	private final MemberRepository memberRepository;
	private final ReservationDatetimeRepository rsvDtRepository;
	private final SeatTypeRepository seatTypeRepository;

	public ReservationService(ReservationRepository repository, MemberRepository memberRepository,
			ReservationDatetimeRepository rsvDtRepository, SeatTypeRepository seatTypeRepository) {

		this.repository = repository;
		this.memberRepository = memberRepository;
		this.rsvDtRepository = rsvDtRepository;
		this.seatTypeRepository = seatTypeRepository;
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

		if (!diningTime.isAfter(LocalDateTime.now())) {
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
		reservation.setRsvCreateTime(LocalDateTime.now());
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

		LocalDateTime now = LocalDateTime.now();

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

		if (!diningTime.toLocalDate().equals(LocalDate.now())) {
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

		if (diningTime.isAfter(LocalDateTime.now())) {
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

		LocalDateTime now = LocalDateTime.now();

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

		LocalDateTime now = LocalDateTime.now();
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

		if (diningTime == null || !diningTime.isAfter(LocalDateTime.now())) {
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

}
