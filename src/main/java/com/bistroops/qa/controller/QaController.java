package com.bistroops.qa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bistroops.qa.model.QaService;
import com.bistroops.qa.model.QaVO;

@Controller
@RequestMapping("/staff/qa")
public class QaController {

	// qa_title 是 VARCHAR(30)
	private static final int TITLE_MAX = 30;

	@Autowired
	private QaService qaService;

	// 公告及QA 的外殼頁，tab = 'qa' 時下方 include QA 列表
	@GetMapping("")
	public String list(Model model) {
		model.addAttribute("qaList", qaService.getAll());
		model.addAttribute("tab", "qa");
		return "staff/announcement-and-qa";
	}

	@GetMapping("/insert")
	public String insertPage() {
		return "staff/qa/insertQaPage";
	}

	@PostMapping("/insert")
	public String insert(@RequestParam String qaTitle, @RequestParam String qaContent, Model model) {

		String errorMsg = validate(qaTitle, qaContent);
		if (errorMsg != null) {
			// 帶著使用者剛剛填的內容回到新增頁
			model.addAttribute("errorMsg", errorMsg);
			model.addAttribute("qaTitle", qaTitle);
			model.addAttribute("qaContent", qaContent);
			return "staff/qa/insertQaPage";
		}

		qaService.insertQa(qaTitle.trim(), qaContent);
		return "redirect:/staff/qa";
	}

	@GetMapping("/update/{qaNo}")
	public String updatePage(@PathVariable Integer qaNo, Model model) {
		QaVO qa = qaService.getQaNoQuery(qaNo);
		if (qa == null) {
			return "redirect:/staff/qa";
		}
		model.addAttribute("qa", qa);
		return "staff/qa/updateQaPage";
	}

	@PostMapping("/update")
	public String update(@RequestParam Integer qaNo, @RequestParam String qaTitle, @RequestParam String qaContent,
			Model model) {

		if (qaService.getQaNoQuery(qaNo) == null) {
			return "redirect:/staff/qa";
		}

		String errorMsg = validate(qaTitle, qaContent);
		if (errorMsg != null) {
			// 另外 new 一個 VO 放使用者剛填的內容，不去改到資料庫查出來的那一筆
			QaVO qa = new QaVO();
			qa.setQaNo(qaNo);
			qa.setQaTitle(qaTitle);
			qa.setQaContent(qaContent);
			model.addAttribute("errorMsg", errorMsg);
			model.addAttribute("qa", qa);
			return "staff/qa/updateQaPage";
		}

		qaService.updateQa(qaNo, qaTitle.trim(), qaContent);
		return "redirect:/staff/qa";
	}

	@PostMapping("/delete/{qaNo}")
	public String delete(@PathVariable Integer qaNo) {
		qaService.deleteQa(qaNo);
		return "redirect:/staff/qa";
	}

	// 沒問題回傳 null，有問題回傳錯誤訊息
	private String validate(String qaTitle, String qaContent) {
		if (qaTitle == null || qaTitle.trim().isEmpty()) {
			return "請輸入問題標題";
		}
		if (qaTitle.trim().length() > TITLE_MAX) {
			return "問題標題最多 " + TITLE_MAX + " 個字";
		}
		if (qaContent == null || qaContent.trim().isEmpty()) {
			return "請輸入回答內容";
		}
		return null;
	}
}
