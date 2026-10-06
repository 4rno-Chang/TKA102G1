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
}