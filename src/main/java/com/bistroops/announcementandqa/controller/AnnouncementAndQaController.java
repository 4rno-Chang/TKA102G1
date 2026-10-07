package com.bistroops.announcementandqa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 側邊選單「公告及QA」的入口：預設顯示公告分頁
// 公告分頁 /staff/announcement（AnnouncementController）、常見問題分頁 /staff/qa（QaController）
@Controller
public class AnnouncementAndQaController {

	@GetMapping("/staff/announcement-and-qa")
	public String index() {
		return "redirect:/staff/announcement";
	}
}
