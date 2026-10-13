package com.bistroops.waiting.config;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bistroops.waiting.redis-check-enabled", havingValue = "true")
public class WaitingRedisCheck implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(WaitingRedisCheck.class);

	private final StringRedisTemplate redisTemplate;

	public WaitingRedisCheck(StringRedisTemplate redisTemplate) {

		this.redisTemplate = redisTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {

		String testKey = "bistroops:waiting:connection-test:" + UUID.randomUUID();

		boolean keyWritten = false;

		try {
			String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());

			if (!"PONG".equalsIgnoreCase(pong)) {
				throw new IllegalStateException("Redis 未回傳 PONG");
			}

			// 使用獨立測試 key，最多保留 60 秒。
			redisTemplate.opsForValue().set(testKey, "connected", Duration.ofSeconds(60));

			keyWritten = true;

			String result = redisTemplate.opsForValue().get(testKey);

			if (!"connected".equals(result)) {
				throw new IllegalStateException("Redis 讀寫結果不一致");
			}

			log.info("【候位 Redis】Spring Boot 連線及讀寫測試成功");

		} catch (RuntimeException ex) {
			// 測試失敗仍讓其他功能啟動。
			// 正式候位操作的故障處理，會在後續 Service 實作。
			log.error("【候位 Redis】連線或讀寫測試失敗，請檢查 Redis 與連線設定", ex);

		} finally {
			if (keyWritten) {
				try {
					redisTemplate.delete(testKey);

				} catch (RuntimeException ex) {
					log.warn("【候位 Redis】測試 key 未能清除，將於到期後失效");
				}
			}
		}
	}
}