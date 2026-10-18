package com.bistroops.employee.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bistroops.employee.model.EmployeeNoteRedisService;

import jakarta.servlet.http.HttpSession;

// 撰寫by陳巧馨
// 後台首頁（templates/staff/staff_index.html）的「工作備忘錄」用：每位員工各自一份，存在 Redis
@Controller
@RequestMapping("/staff/worknote")
public class EmployeeNoteController {

	@Autowired
	private EmployeeNoteRedisService noteService;

	private static final MediaType TEXT_UTF8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);   // 純文字、UTF-8（中文才不會變亂碼）

	// 讀取目前登入員工的備忘錄（GET /staff/worknote）；沒有登入回傳 401
	@GetMapping
	@ResponseBody
	public ResponseEntity<String> getNote(HttpSession session) {
		Integer empNo = (Integer) session.getAttribute("employeeNo");
		if (empNo == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(TEXT_UTF8).body("");
		}
		return ResponseEntity.ok().contentType(TEXT_UTF8).body(noteService.getNote(empNo));
	}

	// 儲存目前登入員工的備忘錄（POST /staff/worknote，欄位 note）；空白就刪除這筆，超過 1000 字只存前 1000 字
	@PostMapping
	@ResponseBody
	public ResponseEntity<String> saveNote(@RequestParam(required = false) String note, HttpSession session) {
		Integer empNo = (Integer) session.getAttribute("employeeNo");
		if (empNo == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(TEXT_UTF8).body("");
		}
		noteService.saveNote(empNo, note);
		return ResponseEntity.ok().contentType(TEXT_UTF8).body("ok");
	}
}
