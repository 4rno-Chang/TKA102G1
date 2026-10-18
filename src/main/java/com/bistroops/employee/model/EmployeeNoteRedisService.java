package com.bistroops.employee.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

// 撰寫by陳巧馨
// 後台主頁的「工作備註欄」，每位員工各存一份在 Redis（不設過期時間）
//   key：staff:worknote:員工編號（例如 staff:worknote:3），值：備註的文字
//   員工編號從登入時存在 session 的 "employeeNo" 取得
@Service
public class EmployeeNoteRedisService {

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static final String KEY_PREFIX = "staff:worknote:";
	public static final int MAX_LENGTH = 1000;   // 備註最多 1000 字（和畫面上填寫框的 maxlength 相同）

	// 取得這位員工的備註；沒有寫過、或員工編號是 null 時回傳空字串
	public String getNote(Integer empNo) {
		if (empNo == null) return "";
		String note = redisTemplate.opsForValue().get(key(empNo));
		return (note == null) ? "" : note;
	}

	// 儲存這位員工的備註：超過 1000 字只存前 1000 字；內容是空的（或只有空白）就把這筆刪掉
	public void saveNote(Integer empNo, String note) {
		if (empNo == null) return;
		if (note == null || note.isBlank()) {
			deleteNote(empNo);
			return;
		}
		if (note.length() > MAX_LENGTH) {
			note = note.substring(0, MAX_LENGTH);
		}
		redisTemplate.opsForValue().set(key(empNo), note);
	}

	// 刪除這位員工的備註（例如員工離職、或想清空時）
	public void deleteNote(Integer empNo) {
		if (empNo == null) return;
		redisTemplate.delete(key(empNo));
	}

	// 組出這位員工的 key
	private String key(Integer empNo) {
		return KEY_PREFIX + empNo;
	}
}
