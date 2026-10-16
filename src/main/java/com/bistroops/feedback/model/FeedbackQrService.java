package com.bistroops.feedback.model;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FeedbackQrService {

	
	// 結帳頁回饋 QR Code 的 token：Redis 存「token → 訂單編號」，7 天後自動刪除
	private static final String KEY = "feedback:qr:";
	
	@Autowired
	StringRedisTemplate redisTempemplate;
	
//	public String createToken(Integer orders) {
//		String token =UUID.randomUUID().toString().replace("_","");
//		redisTemplate.opsForValue().set(KEY=token,String.valueOf(orders));
//		
//		
//	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
