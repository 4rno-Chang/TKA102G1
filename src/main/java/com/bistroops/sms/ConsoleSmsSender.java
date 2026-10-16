package com.bistroops.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 僅將內容輸出到 Console，不呼叫任何簡訊業者。
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

    @Override
    public void send(String phone, String message) {

        log.info(
                "\n"
                + "========== 測試簡訊：未傳送至手機 ==========\n"
                + "收件電話：{}\n"
                + "簡訊內容：{}\n"
                + "==========================================",
                phone,
                message);
    }
}