package com.bistroops.reservationdatetime.config;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bistroops.reservation.model.ReservationService;
import com.bistroops.sms.SmsService;

@Component
public class ReservationReminderScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ReservationReminderScheduler.class);

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Taipei");

    private final ReservationService reservationService;
    private final SmsService smsService;
    private final StringRedisTemplate redisTemplate;
    private final String smsMode;

    public ReservationReminderScheduler(
            ReservationService reservationService,
            SmsService smsService,
            StringRedisTemplate redisTemplate,
            @Value("${app.sms.mode:console}") String smsMode) {

        this.reservationService = reservationService;
        this.smsService = smsService;
        this.redisTemplate = redisTemplate;
        this.smsMode = smsMode;
    }

    public boolean isConsoleMode() {
        return "console".equals(smsMode);
    }

    // 不補發08:00後才新增的訂位。
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Taipei")
    public void scheduledReminder() {

        try {
            ReminderResult result = sendTodayReminders();

            log.info(
                    "今日訂位提醒完成：處理成功 {} 筆、略過 {} 筆、失敗 {} 筆",
                    result.sent(),
                    result.skipped(),
                    result.failed());

        } catch (RuntimeException ex) {
            log.error(
                    "訂位提醒排程未完成，原因類型：{}",
                    ex.getClass().getSimpleName());
        }
    }

    // 供測試按鈕與排程共用。
    public ReminderResult sendTodayReminders() {

        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        List<Map<String, Object>> targets =
                reservationService.getDailyReminderTargets(today);

        int sent = 0;
        int skipped = 0;
        int failed = 0;

        for (Map<String, Object> target : targets) {
            if (!today.equals(LocalDate.now(BUSINESS_ZONE))) {
                throw new IllegalStateException("日期已切換，停止處理");
            }

            String reservationNo =
                    String.valueOf(target.get("reservationNo"));

            // 測試與正式模式分開記錄，避免測試紀錄擋住正式通知。
            String key = "bistroops:sms:reservation:"
                    + smsMode + ":" + today + ":" + reservationNo;

            boolean sendCompleted = false;

            try {
                // 先登記處理中，避免多個執行者同時發送同一筆。
                Boolean claimed = redisTemplate.opsForValue()
                        .setIfAbsent(
                                key,
                                "PROCESSING",
                                Duration.ofDays(2));

                if (!Boolean.TRUE.equals(claimed)) {
                    skipped++;
                    continue;
                }

                String message = String.format(
                        "親愛的顧客您好，您在本日%s有在Bistroops餐廳訂%s位，我們恭候您前來用餐。",
                        target.get("timeText"),
                        target.get("tableType"));

                smsService.send(
                        String.valueOf(target.get("phone")),
                        message);

                sendCompleted = true;

                redisTemplate.opsForValue().set(
                        key,
                        "SENT",
                        Duration.ofDays(2));

                sent++;

            } catch (RuntimeException ex) {
                failed++;

                // 已完成發送但紀錄失敗時，保留原標記，
                // 避免直接重送造成重複通知。
                if (!sendCompleted) {
                    try {
                        redisTemplate.delete(key);
                    } catch (RuntimeException ignored) {
                        log.warn(
                                "無法清除提醒處理標記，訂位編號：{}",
                                reservationNo);
                    }
                }

                log.warn(
                        "訂位提醒未完成，訂位編號：{}，原因類型：{}",
                        reservationNo,
                        ex.getClass().getSimpleName());
            }
        }

        return new ReminderResult(sent, skipped, failed);
    }

    public record ReminderResult(
            int sent,
            int skipped,
            int failed) {
    }
}