package com.bistroops.sms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

/**
 * 測試模式：
 * 1. 保留Console紀錄。
 * 2. 網頁操作產生的訊息，只放入該瀏覽器的Session。
 * 3. 排程訊息另外保存，由有權限的後台讀取。
 */
@Component
@ConditionalOnProperty(
        prefix = "app.sms",
        name = "mode",
        havingValue = "console",
        matchIfMissing = true)
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log =
            LoggerFactory.getLogger(ConsoleSmsSender.class);

    private static final String SESSION_MESSAGES =
            "bistroops.simulatedSmsMessages";

    private static final long RETENTION_MILLIS =
            24 * 60 * 60 * 1000L;

    private final String streamId = UUID.randomUUID().toString();
    private final AtomicLong sequence = new AtomicLong();

    private final List<PreviewMessage> scheduledMessages =
            new ArrayList<>();

    @Override
    public void send(String phone, String message) {

        log.info(
                "\n========== 模擬簡訊：未實際發送 ==========\n"
                        + "收件電話：{}\n"
                        + "簡訊內容：{}\n"
                        + "========================================",
                phone,
                message);

        var attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes web) {
            HttpSession session = web.getRequest().getSession();

            synchronized (session) {
                List<PreviewMessage> messages = sessionMessages(session);

                messages.add(new PreviewMessage(
                        sequence.incrementAndGet(),
                        phone,
                        message,
                        System.currentTimeMillis()));

                trim(messages, 50);
            }

        } else {
            synchronized (scheduledMessages) {
                scheduledMessages.add(new PreviewMessage(
                        sequence.incrementAndGet(),
                        phone,
                        message,
                        System.currentTimeMillis()));

                trim(scheduledMessages, 100);
            }
        }
    }

    public String getStreamId() {
        return streamId;
    }

    public List<PreviewMessage> readMessages(
            HttpSession session,
            boolean includeScheduled) {

        List<PreviewMessage> result = new ArrayList<>();

        if (session != null) {
            synchronized (session) {
                List<PreviewMessage> messages = sessionMessages(session);
                trim(messages, 50);
                result.addAll(messages);
            }
        }

        if (includeScheduled) {
            synchronized (scheduledMessages) {
                trim(scheduledMessages, 100);
                result.addAll(scheduledMessages);
            }
        }

        result.sort(Comparator.comparingLong(PreviewMessage::id));

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<PreviewMessage> sessionMessages(HttpSession session) {

        Object stored = session.getAttribute(SESSION_MESSAGES);

        if (stored instanceof List<?>) {
            return (List<PreviewMessage>) stored;
        }

        List<PreviewMessage> messages = new ArrayList<>();
        session.setAttribute(SESSION_MESSAGES, messages);

        return messages;
    }

    private void trim(List<PreviewMessage> messages, int limit) {

        long cutoff = System.currentTimeMillis() - RETENTION_MILLIS;

        messages.removeIf(message -> message.createdAt() < cutoff);

        while (messages.size() > limit) {
            messages.remove(0);
        }
    }

    public record PreviewMessage(
            long id,
            String phone,
            String message,
            long createdAt) {
    }
}