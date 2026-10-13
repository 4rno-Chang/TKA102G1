package com.bistroops.reservationdatetime.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Modifying;

public interface ReservationDatetimeRepository extends JpaRepository<ReservationDatetimeVO, Long> {

	List<ReservationDatetimeVO> findByRsvDtDatetimeGreaterThanEqualAndRsvDtDatetimeLessThanOrderByRsvDtDatetimeAsc(
			LocalDateTime startTime, LocalDateTime endTime);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT dt
			FROM ReservationDatetimeVO dt
			WHERE dt.rsvDtNo = :rsvDtNo
			""")
	Optional<ReservationDatetimeVO> findByIdForUpdate(@Param("rsvDtNo") Long rsvDtNo);

	// 補齊起始日期起算90天，每天四個時段。
	// 已存在的時間保留原本編號。
	@Modifying
	@Query(value = """
			INSERT INTO reservation_datetime (rsv_dt_datetime)
			SELECT candidates.dining_time
			FROM (
			    SELECT TIMESTAMP(
			        DATE_ADD(
			            :startDate,
			            INTERVAL (ones.n + tens.n * 10) DAY
			        ),
			        slots.slot_time
			    ) AS dining_time
			    FROM (
			        SELECT 0 AS n UNION ALL SELECT 1
			        UNION ALL SELECT 2 UNION ALL SELECT 3
			        UNION ALL SELECT 4 UNION ALL SELECT 5
			        UNION ALL SELECT 6 UNION ALL SELECT 7
			        UNION ALL SELECT 8 UNION ALL SELECT 9
			    ) ones
			    CROSS JOIN (
			        SELECT 0 AS n UNION ALL SELECT 1
			        UNION ALL SELECT 2 UNION ALL SELECT 3
			        UNION ALL SELECT 4 UNION ALL SELECT 5
			        UNION ALL SELECT 6 UNION ALL SELECT 7
			        UNION ALL SELECT 8
			    ) tens
			    CROSS JOIN (
			        SELECT '11:00:00' AS slot_time
			        UNION ALL SELECT '12:30:00'
			        UNION ALL SELECT '17:00:00'
			        UNION ALL SELECT '18:30:00'
			    ) slots
			) candidates
			WHERE NOT EXISTS (
			    SELECT 1
			    FROM reservation_datetime existing
			    WHERE existing.rsv_dt_datetime = candidates.dining_time
			)
			ORDER BY candidates.dining_time
			ON DUPLICATE KEY UPDATE
			    rsv_dt_datetime = reservation_datetime.rsv_dt_datetime
			""", nativeQuery = true)
	int ensureRollingWindow(@Param("startDate") LocalDate startDate);
}