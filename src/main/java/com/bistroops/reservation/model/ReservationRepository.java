package com.bistroops.reservation.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<ReservationVO, Long> {

	@Override
	@EntityGraph(attributePaths = { "member", "rsvDt", "seatType" })
	List<ReservationVO> findAll();

	@EntityGraph(attributePaths = { "member", "rsvDt", "seatType" })
	@Query("""
			SELECT r
			FROM Rsv r
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

}
