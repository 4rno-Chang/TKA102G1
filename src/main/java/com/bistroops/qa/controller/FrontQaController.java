package com.bistroops.qa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bistroops.qa.model.QaService;

// 前台(顧客)常見問題：首頁 FAQ 的「more→」和 header、footer 的「常見問題」連到這裡
@Controller
@RequestMapping("/bistroops/qa")
public class FrontQaController {

	@Autowired
	private QaService qaService;

	// 全部常見問題，點擊多的在前
	@GetMapping("")
	public String list(Model model) {
		model.addAttribute("qaList", qaService.getAllPopularFirst());
		return "front/qa/listAll";
	}

	// 首頁或常見問題頁展開一題時，qa-click.js 送來這裡，點擊次數 +1；不需要畫面，回 204
	@PostMapping("/{qaNo}/click")
	public ResponseEntity<Void> click(@PathVariable Integer qaNo) {
		qaService.addClick(qaNo);
		return ResponseEntity.noContent().build();
	}
}
