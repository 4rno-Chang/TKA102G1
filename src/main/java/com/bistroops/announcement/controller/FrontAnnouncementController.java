package com.bistroops.announcement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bistroops.announcement.model.AnnouncementService;
import com.bistroops.announcement.model.AnnouncementVO;

// 前台(顧客)公告：只能瀏覽已開始的公告
@Controller
@RequestMapping("/bistroops/announcement")
public class FrontAnnouncementController {

	@Autowired
	private AnnouncementService annService;

	@GetMapping("")
	public String list(Model model) {
		model.addAttribute("annList", annService.getPublished());
		return "front/announcement/listAll";
	}

	@GetMapping("/{annNo}")
	public String detail(@PathVariable Integer annNo, Model model) {
		AnnouncementVO ann = annService.getPublishedByNo(annNo);

		// 查無資料或尚未開始，導回公告列表
		if (ann == null) {
			return "redirect:/bistroops/announcement";
		}

		model.addAttribute("ann", ann);
		return "front/announcement/detail";
	}

	@GetMapping("/image/{annNo}")
	public ResponseEntity<byte[]> image(@PathVariable Integer annNo) {
		AnnouncementVO ann = annService.getPublishedByNo(annNo);
		byte[] img = (ann != null) ? ann.getAnnImg() : null;

		if (img == null || img.length == 0) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).header(HttpHeaders.CACHE_CONTROL, "no-cache")
				.body(img);
	}

}
