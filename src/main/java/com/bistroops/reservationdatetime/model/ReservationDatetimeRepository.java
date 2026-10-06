package com.bistroops.reservationdatetime.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationDatetimeRepository extends JpaRepository<ReservationDatetimeVO, Long> {

	List<ReservationDatetimeVO> findByRsvDtDatetimeGreaterThanEqualAndRsvDtDatetimeLessThanOrderByRsvDtDatetimeAsc(
			LocalDateTime startTime, LocalDateTime endTime);
}