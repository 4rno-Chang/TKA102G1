package com.bistroops.reservationdatetime.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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
}