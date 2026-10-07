package com.bistroops.member.model;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    // Redis 的 Key 前綴
    private static final String CAPTCHA_PREFIX = "captcha:";

    // 驗證碼有效時間：2 分鐘
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);

    // 驗證碼使用 0～9 純數字
    private static final String CHARS ="0123456789";

    private final StringRedisTemplate redisTemplate;

    private final SecureRandom random = new SecureRandom();

    public CaptchaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 產生 4 碼驗證碼，並存進 Redis
    public String createCaptcha(String sessionId) {

        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            int index = random.nextInt(CHARS.length());
            code.append(CHARS.charAt(index));
        }

        String captcha = code.toString();

        // 例如：
        // captcha:ABC123SESSION -> 5832
        redisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + sessionId,
                captcha,
                CAPTCHA_TTL
        );

        return captcha;
    }

    // 驗證使用者輸入的驗證碼
    public boolean verifyCaptcha(String sessionId, String input) {

        if (input == null || input.isBlank()) {
            return false;
        }

        String key = CAPTCHA_PREFIX + sessionId;

        String correctCaptcha =
                redisTemplate.opsForValue().get(key);

        if (correctCaptcha == null) {
            return false;
        }

        // 驗證碼只能使用一次
        redisTemplate.delete(key);

        return correctCaptcha.equals(input.trim());
    }
}