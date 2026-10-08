package com.bistroops.reservationdatetime.model;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ReservationDatetimeService {

	private final ReservationDatetimeRepository repository;

	public ReservationDatetimeService(ReservationDatetimeRepository repository) {
		this.repository = repository;
	}

	public List<ReservationDatetimeVO> getAll() {
		return repository.findAll(Sort.by("rsvDtDatetime").ascending());
	}

	public List<ReservationDatetimeVO> getAvailableByDate(LocalDate date) {
		if (date.isBefore(LocalDate.now())) {
			return List.of();
		}

		LocalDateTime now = LocalDateTime.now();

		Set<LocalTime> allowedTimes = Set.of(LocalTime.of(11, 0), LocalTime.of(12, 30), LocalTime.of(17, 0),
				LocalTime.of(18, 30));

		return repository
				.findByRsvDtDatetimeGreaterThanEqualAndRsvDtDatetimeLessThanOrderByRsvDtDatetimeAsc(date.atStartOfDay(),
						date.plusDays(1).atStartOfDay())
				.stream().filter(slot -> slot.getRsvDtDatetime().isAfter(now))
				.filter(slot -> allowedTimes.contains(slot.getRsvDtDatetime().toLocalTime())).toList();
	}

	public List<LocalDate> getOpenDates() {
		LocalDate today = LocalDate.now();

		Set<LocalTime> allowedTimes = Set.of(LocalTime.of(11, 0), LocalTime.of(12, 30), LocalTime.of(17, 0),
				LocalTime.of(18, 30));

		LocalDateTime now = LocalDateTime.now();

		return repository
				.findByRsvDtDatetimeGreaterThanEqualAndRsvDtDatetimeLessThanOrderByRsvDtDatetimeAsc(
						today.atStartOfDay(), today.plusMonths(3).plusDays(1).atStartOfDay())
				.stream().filter(slot -> slot.getRsvDtDatetime() != null).map(ReservationDatetimeVO::getRsvDtDatetime)
				.filter(time -> time.isAfter(now)).filter(time -> allowedTimes.contains(time.toLocalTime()))
				.map(LocalDateTime::toLocalDate).distinct().sorted().toList();
	}

}
