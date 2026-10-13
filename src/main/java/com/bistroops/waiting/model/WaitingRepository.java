package com.bistroops.waiting.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;

public interface WaitingRepository extends JpaRepository<WaitingVO, Long> {

	List<WaitingVO> findAllByOrderByWaitingNoAsc();

	List<WaitingVO> findBySeatType_SeatTypeNoAndWaitingStatusInOrderByWaitingNoAsc(Integer seatTypeNo,
			Collection<String> statuses);

	List<WaitingVO> findByMember_MemNoOrderByWaitingNoDesc(Integer memNo);

	long countBySeatType_SeatTypeNoAndWaitingStatusIn(Integer seatTypeNo, Collection<String> statuses);

	long countBySeatType_SeatTypeNoAndWaitingNoLessThanAndWaitingStatusIn(Integer seatTypeNo, Long waitingNo,
			Collection<String> statuses);

	boolean existsBySeatType_SeatTypeNoAndWaitingTelAndWaitingStatusIn(Integer seatTypeNo, String waitingTel,
			Collection<String> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT w FROM WaitingVO w WHERE w.waitingNo = :waitingNo")
	Optional<WaitingVO> findByIdForUpdate(@Param("waitingNo") Long waitingNo);

	// 以資料庫桌型資料作為取號鎖，避免同桌型、同電話同時重複送出。
	// nativeQuery 使用資料表欄位名稱；其餘方法依 Java 屬性查詢。
	@Query(value = """
			SELECT seat_type_no
			FROM seat_type
			WHERE seat_type_no = :seatTypeNo
			FOR UPDATE
			""", nativeQuery = true)
	Optional<Integer> lockSeatType(@Param("seatTypeNo") Integer seatTypeNo);

	// 後台查詢某一天的全部候位。
	@EntityGraph(attributePaths = { "member", "seatType" })
	List<WaitingVO> findByWaitingDateOrderByWaitingTakeTimeAscWaitingNoAsc(LocalDate waitingDate);

	// 查詢同一天、同桌型的有效候位。
	@EntityGraph(attributePaths = { "member", "seatType" })
	List<WaitingVO> findByWaitingDateAndSeatType_SeatTypeNoAndWaitingStatusInOrderByWaitingTicketSeqAsc(
			LocalDate waitingDate, Integer seatTypeNo, Collection<String> statuses);

	// 計算當日某桌型的有效候位組數。
	long countByWaitingDateAndSeatType_SeatTypeNoAndWaitingStatusIn(LocalDate waitingDate, Integer seatTypeNo,
			Collection<String> statuses);

	// 計算同日期、同桌型、較早號碼的有效候位組數。
	long countByWaitingDateAndSeatType_SeatTypeNoAndWaitingTicketSeqLessThanAndWaitingStatusIn(LocalDate waitingDate,
			Integer seatTypeNo, Integer waitingTicketSeq, Collection<String> statuses);

	// 檢查同一天、同桌型、同電話是否已有有效候位。
	boolean existsByWaitingDateAndSeatType_SeatTypeNoAndWaitingTelAndWaitingStatusIn(LocalDate waitingDate,
			Integer seatTypeNo, String waitingTel, Collection<String> statuses);

	// 取得當日某桌型最後發出的號碼。
	// 包括取消、過號與已入座紀錄，避免重用已發出的號碼。
	@Query("""
			SELECT COALESCE(MAX(w.waitingTicketSeq), 0)
			FROM WaitingVO w
			WHERE w.waitingDate = :waitingDate
			  AND w.seatType.seatTypeNo = :seatTypeNo
			""")
	Integer findMaxTicketSeq(@Param("waitingDate") LocalDate waitingDate, @Param("seatTypeNo") Integer seatTypeNo);
}
