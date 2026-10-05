package com.bistroops.reservation.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

	private final ReservationRepository repository;

	public ReservationService(ReservationRepository repository) {
		this.repository = repository;
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
}
