package com.bistroops.feedback.model;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bistroops.orders.model.OrdersService;
import com.bistroops.orders.model.OrdersVO;

@Service
public class FeedbackQrService {

	
	// 結帳頁回饋 QR Code 的 token：Redis 存「token → 訂單編號」，7 天後自動刪除
	private static final String KEY = "feedback:qr:";
	
	@Autowired
	StringRedisTemplate redisTemplate;
	
	@Autowired
    OrdersService ordersService;
	
	// 產生一組隨機 token（每按一次就產生新的）
	public String createToken(Integer orders) {
		String token =UUID.randomUUID().toString().replace("_","");
		redisTemplate.opsForValue().set(KEY+token,String.valueOf(orders),
		Duration.ofDays(FeedbackVO.FEEDBACK_DAYS));
		return token;
	}
	// 透過 token 查出訂單編號（如果 token 不存在就回傳 null）
	public OrdersVO findOrder(String token) {
		if(token ==null || token.isBlank())return null;
		String orders=redisTemplate.opsForValue().get(KEY+token);
		return orders==null?null:ordersService.getOne(Integer.valueOf(orders));
	}
	
	// 回饋送出後讓 token 作廢
	public void useToken(String token) {
		if(token ==null || token.isBlank())return;
		redisTemplate.delete(KEY+token);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
