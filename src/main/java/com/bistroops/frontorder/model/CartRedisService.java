package com.bistroops.frontorder.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

// 購物車還沒送出前存在 Redis 關鍵字如下 
//   命令提示字元打 redis-cli，再打：
//       KEYS order:cart:*                  ← 有哪些購物車
//       HGETALL order:cart:A01:183042      ← 某一個購物車的內容（欄位、值交錯列出來）
//       TTL order:cart:A01:183042          ← 還剩幾秒會過期
@Service
public class CartRedisService {

	@Autowired
	private StringRedisTemplate redisTemplate;
	// 每一品項最多幾份
	public static final int MAX_NUM = 20;
	// 購物車保留 3 小時（每次寫入都重新算）
	private static final Duration KEEP = Duration.ofHours(3);  

	// 這一組客人的購物車的 key，例如 "order:cart:A01:183042"
	private String cartKey(String seatingKey) {
		return "order:cart:" + seatingKey;
	}

	// 備註整理成固定的樣子：null 當作沒有備註（空字串）、去掉前後空白
	private String clean(String comment) {
		return comment == null ? "" : comment.trim();
	}

	// 一列的欄位名稱：「菜品編號|備註」，例如 "6|要辣"、"6|"
	private String field(Integer mealNo, String comment) {
		return mealNo + "|" + clean(comment);
	}

	// 重新設定過期時間（每次寫入之後都呼叫：客人有在動，就再保留 3 小時）
	private void touch(String seatingKey) {
		redisTemplate.expire(cartKey(seatingKey), KEEP);
	}

	private void addNum(String seatingKey, String field, int num) {
		Long total = redisTemplate.opsForHash().increment(cartKey(seatingKey), field, num);
		if (total > MAX_NUM) {
			redisTemplate.opsForHash().put(cartKey(seatingKey), field, String.valueOf(MAX_NUM));
		}
	}

	public void add(String seatingKey, Integer mealNo, int num, String comment) {
		addNum(seatingKey, field(mealNo, comment), num);
		
		touch(seatingKey);
	}

	public List<CartLineDTO> getLines(String seatingKey) {
		Map<Object, Object> raw = redisTemplate.opsForHash().entries(cartKey(seatingKey));

		List<CartLineDTO> lines = new ArrayList<>();
		for (Map.Entry<Object, Object> e : raw.entrySet()) {
			String field = (String) e.getKey(); 
			int bar = field.indexOf('|'); 
			if (bar < 0) {
				continue; // 不是這個格式的壞資料：跳過
			}
			try {
				Integer mealNo = Integer.valueOf(field.substring(0, bar));    // | 前面：菜品編號
				String comment = field.substring(bar + 1);                     // | 後面：備註
				Integer num = Integer.valueOf((String) e.getValue());          // 值：數量
				if (num > 0) {
					lines.add(new CartLineDTO(mealNo, comment, num));
				}
			} catch (NumberFormatException ex) {
				System.out.println(ex);
			}
		}
		//排列
		lines.sort(Comparator.comparing(CartLineDTO::getMealNo).thenComparing(CartLineDTO::getComment));
		return lines;
	}
	
	public void update(String seatingKey, Integer mealNo, String oldComment, int num, String newComment) {
		String oldField = field(mealNo, oldComment);
		String newField = field(mealNo, newComment);

		if (!redisTemplate.opsForHash().hasKey(cartKey(seatingKey), oldField)) {
			return;
		}
		if (num <= 0) {
			remove(seatingKey, mealNo, oldComment);
			return;
		}
		if (oldField.equals(newField)) {
			redisTemplate.opsForHash().put(cartKey(seatingKey), oldField, String.valueOf(Math.min(num, MAX_NUM)));
		} else {
			redisTemplate.opsForHash().delete(cartKey(seatingKey), oldField);
			addNum(seatingKey, newField, num);
		}
		touch(seatingKey);
	}
	

	public void remove(String seatingKey, Integer mealNo, String comment) {
		redisTemplate.opsForHash().delete(cartKey(seatingKey), field(mealNo, comment));
	}

	public void clear(String seatingKey) {
		redisTemplate.delete(cartKey(seatingKey));
	}
}
