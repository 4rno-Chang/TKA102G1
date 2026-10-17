package com.bistroops.promote.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bistroops.announcement.model.NewsArticleService;
import com.bistroops.promote.model.PromoteService;
import com.bistroops.promote.model.PromoteVO;

// 前台(顧客)活動：首頁輪播點進來的活動內容，只能看進行中的活動
// 公告負責人新增（活動是 ishaya 的範圍，這支是約定的特例）：PromoteController 都在 /staff 底下要登入，前台讀不到，
// 所以另外開這支；只呼叫 PromoteService，不改它。頁面和公告共用 front/announcement/news-detail.html
@Controller
@RequestMapping("/bistroops/promote")
public class FrontPromoteController {

	@Autowired
	private PromoteService promoteService;

	@Autowired
	private NewsArticleService newsArticleService;

	@GetMapping("/{promoteNo}")
	public String detail(@PathVariable Integer promoteNo, Model model) {
		PromoteVO promote = getActive(promoteNo);

		// 查無資料、未開始或已結束，導回最新消息
		if (promote == null) {
			return "redirect:/bistroops/announcement";
		}

		// news-detail.html 公告和活動共用，所以整理成同樣欄位的 article（見 NewsArticleService）
		model.addAttribute("article", newsArticleService.toArticle(promote));
		return "front/announcement/news-detail";
	}

	@GetMapping("/img")
	public ResponseEntity<byte[]> image(@RequestParam Integer promoteNo) {
		PromoteVO promote = getActive(promoteNo);
		byte[] img = (promote != null) ? promote.getPromoteImg() : null;

		if (img == null || img.length == 0) {
			return ResponseEntity.status(HttpStatus.FOUND)
					.location(URI.create("/images/no-image.svg")).build();
		}

		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).header(HttpHeaders.CACHE_CONTROL, "no-cache")
				.body(img);
	}

	// 只回傳進行中的活動；其他狀態（未開始、已結束、已刪除）或查不到都回傳 null
	private PromoteVO getActive(Integer promoteNo) {
		PromoteVO promote = promoteService.getPromoteByNo(promoteNo);
		if (promote == null || !"進行中".equals(promoteService.getStatus(promote))) {
			return null;
		}
		return promote;
	}
}
