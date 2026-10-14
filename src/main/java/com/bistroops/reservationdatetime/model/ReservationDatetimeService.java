package com.bistroops.reservationdatetime.model;

import java.util.List;
import java.time.ZoneId;

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

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Taipei");

	private final ReservationDatetimeRepository repository;

	public ReservationDatetimeService(ReservationDatetimeRepository repository) {
		this.repository = repository;
	}

	public List<ReservationDatetimeVO> getAll() {
		return repository.findAll(Sort.by("rsvDtDatetime").ascending());
	}

	public List<ReservationDatetimeVO> getAvailableByDate(LocalDate date) {
		if (date == null || date.isBefore(LocalDate.now(BUSINESS_ZONE))) {
			return List.of();
		}

		LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);

		Set<LocalTime> allowedTimes = Set.of(LocalTime.of(11, 0), LocalTime.of(12, 30), LocalTime.of(17, 0),
				LocalTime.of(18, 30));

		return repository
				.findByRsvDtDatetimeGreaterThanEqualAndRsvDtDatetimeLessThanOrderByRsvDtDatetimeAsc(date.atStartOfDay(),
						date.plusDays(1).atStartOfDay())
				.stream().filter(slot -> slot.getRsvDtDatetime().isAfter(now))
				.filter(slot -> allowedTimes.contains(slot.getRsvDtDatetime().toLocalTime())).toList();
	}

	public List<LocalDate> getOpenDates() {

		LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
		LocalDate today = now.toLocalDate();

		Set<LocalTime> allowedTimes = Set.of(LocalTime.of(11, 0), LocalTime.of(12, 30), LocalTime.of(17, 0),
				LocalTime.of(18, 30));

		return repository
				.findByRsvDtDatetimeGreaterThanEqualAndRsvDtDatetimeLessThanOrderByRsvDtDatetimeAsc(
						today.atStartOfDay(), today.plusDays(90).atStartOfDay())
				.stream().filter(slot -> slot.getRsvDtDatetime() != null)
				.filter(slot -> slot.getRsvDtDatetime().isAfter(now))
				.filter(slot -> allowedTimes.contains(slot.getRsvDtDatetime().toLocalTime()))
				.map(slot -> slot.getRsvDtDatetime().toLocalDate()).distinct().sorted().toList();
	}

}
