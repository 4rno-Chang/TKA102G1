package com.bistroops.reservationdatetime.model;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationSlotWindowService {

	private final ReservationDatetimeRepository repository;

	public ReservationSlotWindowService(ReservationDatetimeRepository repository) {

		this.repository = repository;
	}

	// 使用獨立寫入交易。
	// 即使由唯讀的訂位查詢方法呼叫，也能補齊時段。
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void ensureWindow(LocalDate startDate) {

		repository.ensureRollingWindow(startDate);
	}
}