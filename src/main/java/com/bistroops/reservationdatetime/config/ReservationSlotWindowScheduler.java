package com.bistroops.reservationdatetime.config;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;

import com.bistroops.reservationdatetime.model.ReservationSlotWindowService;

@Component
@EnableScheduling
public class ReservationSlotWindowScheduler {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Taipei");

	private final ReservationSlotWindowService windowService;

	public ReservationSlotWindowScheduler(ReservationSlotWindowService windowService) {

		this.windowService = windowService;
	}

	// 啟動完成時補齊時段。
	@EventListener(ApplicationReadyEvent.class)
	public void initializeWindow() {
		refreshWindow();
	}

	// 每天台灣時間00:00補齊。
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Taipei")
	public void refreshWindow() {

		windowService.ensureWindow(LocalDate.now(BUSINESS_ZONE));
	}
}