package com.bistroops.meal.model;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

// 菜單頁的「類別代表圖」設定，存在 Redis（不設過期時間）
//   一個 Hash：key 是 "menu:cover"，欄位是類別編號、值是菜品編號
@Service
public class MenuCoverRedisService {
	
	@Autowired 
	private StringRedisTemplate redisTemplate;
	
	private static final String KEY = "menu:cover";
	
	public void setCover(Integer mealtypeNo, Integer mealNo) {
		redisTemplate.opsForHash().put(KEY, String.valueOf(mealtypeNo), String.valueOf(mealNo));
	}
	
	// 取消某個類別的代表圖設定（回到自動挑選）。目前畫面沒有使用，保留備用
	public void removeCover(Integer mealtypeNo) {
		redisTemplate.opsForHash().delete(KEY, String.valueOf(mealtypeNo));
	}
	
	public Map<Integer, Integer> getCovers() {
	    return redisTemplate.<String, String>opsForHash().entries(KEY).entrySet().stream()
	            .filter(e -> e.getKey().matches("[0-9]+") && e.getValue().matches("[0-9]+"))   // 只留數字（跳過壞資料）
	            .collect(Collectors.toMap(
	                    e -> Integer.valueOf(e.getKey()),
	                    e -> Integer.valueOf(e.getValue())));
	}

}
