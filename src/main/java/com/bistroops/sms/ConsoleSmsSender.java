package com.bistroops.sms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

@Component
@ConditionalOnProperty(
        prefix = "app.sms",
        name = "mode",
        havingValue = "console",
        matchIfMissing = true)
public class ConsoleSmsSender implements SmsSender {

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

        PreviewMessage preview = new PreviewMessage(
                sequence.incrementAndGet(),
                phone,
                message,
                System.currentTimeMillis());

        var attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes web) {

            HttpSession session = web.getRequest().getSession();

            synchronized (session) {
                List<PreviewMessage> messages = copyMessages(session);

                messages.add(preview);
                trim(messages, 50);

                // 明確更新屬性，讓 Redis Session 儲存訊息。
                session.setAttribute(SESSION_MESSAGES, messages);
            }

        } else {
            // 排程訊息由具備權限的後台讀取。
            synchronized (scheduledMessages) {
                scheduledMessages.add(preview);
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
            List<PreviewMessage> messages = copyMessages(session);
            trim(messages, 50);
            result.addAll(messages);
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

    private List<PreviewMessage> copyMessages(HttpSession session) {

        List<PreviewMessage> result = new ArrayList<>();

        Object stored = session.getAttribute(SESSION_MESSAGES);

        if (stored instanceof List<?> messages) {
            for (Object item : messages) {
                if (item instanceof PreviewMessage preview) {
                    result.add(preview);
                }
            }
        }

        return result;
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
            long createdAt) implements Serializable {

        private static final long serialVersionUID = 1L;
    }
}