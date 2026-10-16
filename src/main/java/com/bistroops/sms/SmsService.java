package com.bistroops.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log =
            LoggerFactory.getLogger(SmsService.class);

    private final SmsSender sender;

    public SmsService(SmsSender sender) {
        this.sender = sender;
    }

    /**
     * 發送必要訊息，例如會員驗證碼。
     * 失敗時拋出例外，由呼叫端顯示錯誤。
     */
    public void send(String phone, String message) {

        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedMessage = message == null ? "" : message.trim();

        if (!normalizedPhone.matches("09[0-9]{8}")) {
            throw new SmsSendException("手機號碼格式錯誤");
        }

        if (normalizedMessage.isEmpty()) {
            throw new SmsSendException("簡訊內容不能為空");
        }

        try {
            sender.send(normalizedPhone, normalizedMessage);

        } catch (SmsSendException ex) {
            throw ex;

        } catch (RuntimeException ex) {
            throw new SmsSendException(
                    "簡訊暫時無法發送，請稍後再試",
                    ex);
        }
    }

    /**
     * 發送附加通知，例如訂位成功、候位叫號。
     * 失敗時回傳 false，不推翻已完成的訂位或叫號。
     */
    public boolean trySend(String phone, String message) {

        try {
            send(phone, message);
            return true;

        } catch (SmsSendException ex) {
            log.warn(
                    "簡訊通知未完成，電話末四碼：{}，原因類型：{}",
                    lastFourDigits(phone),
                    ex.getClass().getSimpleName());

            return false;
        }
    }

    private String lastFourDigits(String phone) {

        if (phone == null || phone.length() < 4) {
            return "未知";
        }

        return phone.substring(phone.length() - 4);
    }
}