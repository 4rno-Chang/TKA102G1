package com.bistroops.reservation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<ReservationVO, Long> {

	// 查詢全部訂位，並一起載入會員、訂位時段與桌型。
	@Override
	@EntityGraph(attributePaths = { "member", "rsvDt", "seatType" })
	List<ReservationVO> findAll();

	// 查詢單筆訂位，提供明細頁面與結果頁使用。
	@Override
	@EntityGraph(attributePaths = { "member", "rsvDt", "seatType" })
	Optional<ReservationVO> findById(Long rsvNo);

	// 後台依用餐日期與訂位狀態查詢。
	// 參數為 null 時，不套用該項篩選。
	@EntityGraph(attributePaths = { "member", "rsvDt", "seatType" })
	@Query("""
			SELECT r
			FROM ReservationVO r
			JOIN r.rsvDt dt
			WHERE (:startTime IS NULL
			       OR dt.rsvDtDatetime >= :startTime)
			  AND (:endTime IS NULL
			       OR dt.rsvDtDatetime < :endTime)
			  AND (:status IS NULL
			       OR r.rsvStatus = :status)
			ORDER BY dt.rsvDtDatetime ASC, r.rsvNo ASC
			""")
	List<ReservationVO> search(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
			@Param("status") String status);

	// 計算指定時段、指定桌型已占用的訂位桌數。
	// 取消的訂位不占用名額。
	@Query("""
			SELECT COUNT(r)
			FROM ReservationVO r
			WHERE r.rsvDt.rsvDtNo = :rsvDtNo
			  AND r.seatType.seatTypeNo = :seatTypeNo
			  AND r.rsvStatus IN ('預約', '實到')
			""")
	long countActiveByTimeAndSeatType(@Param("rsvDtNo") Long rsvDtNo, @Param("seatTypeNo") Integer seatTypeNo);

	// 檢查同一會員是否已在同一時段訂位。
	@Query("""
			SELECT COUNT(r)
			FROM ReservationVO r
			WHERE r.member.memNo = :memNo
			  AND r.rsvDt.rsvDtNo = :rsvDtNo
			  AND r.rsvStatus IN ('預約', '實到')
			""")
	long countActiveByMemberAndTime(@Param("memNo") Integer memNo, @Param("rsvDtNo") Long rsvDtNo);

	// 前台「我的訂位」使用，較新的用餐日期排在前面。
	@EntityGraph(attributePaths = { "member", "rsvDt", "seatType" })
	@Query("""
			SELECT r
			FROM ReservationVO r
			WHERE r.member.memNo = :memNo
			ORDER BY r.rsvDt.rsvDtDatetime DESC, r.rsvNo DESC
			""")
	List<ReservationVO> findByMemberNo(@Param("memNo") Integer memNo);

	// 修改或取消前，鎖定這筆訂位。
	// 必須由具有 @Transactional 的 Service 方法呼叫。
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT r
			FROM ReservationVO r
			WHERE r.rsvNo = :rsvNo
			""")
	Optional<ReservationVO> findByIdForUpdate(@Param("rsvNo") Long rsvNo);

	// 查詢指定範圍內仍為「預約」的訂位。
	// 後續提供後台首頁「最近一輪訂位」統計使用。
	@EntityGraph(attributePaths = { "member", "rsvDt", "seatType" })
	@Query("""
			SELECT r
			FROM ReservationVO r
			WHERE r.rsvStatus = '預約'
			  AND r.rsvDt.rsvDtDatetime >= :startTime
			  AND r.rsvDt.rsvDtDatetime < :endTime
			ORDER BY r.rsvDt.rsvDtDatetime ASC, r.rsvNo ASC
			""")
	List<ReservationVO> findUpcomingReservations(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);
}