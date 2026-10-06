package com.bistroops.announcement.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.bistroops.announcement.model.AnnouncementService;
import com.bistroops.announcement.model.AnnouncementVO;

@Controller
@RequestMapping("/staff/announcement")
public class AnnouncementController {

	@Autowired
	private AnnouncementService annService;
	
	@GetMapping("")
	public String index() {
		return "staff/announcement/index";
	}
    
    @GetMapping("/list")
	public String list(Model model) {
		model.addAttribute("annList", annService.getAll());
		return "staff/announcement/listAllAnns";
	} 
    
	@GetMapping("/search")
	public String search(@RequestParam(name = "annNo", required = false) String annNoStr, Model model) {
		if (annNoStr == null || annNoStr.trim().isEmpty()) {
			model.addAttribute("errorMsg", "請輸入公告編號");
			return "staff/announcement/index";
		}

		try {
			Integer annNo = Integer.parseInt(annNoStr.trim());
			AnnouncementVO ann = annService.getAnnNoQuery(annNo);

			if (ann == null) {
				model.addAttribute("errorMsg", "查無此公告編號：" + annNo);
				return "staff/announcement/index";
			}
			model.addAttribute("ann", ann);
			return "staff/announcement/listOneAnn";

		} catch (NumberFormatException e) {
			model.addAttribute("errorMsg", "公告編號格式錯誤");
			return "staff/announcement/index";
		}
	}

	@GetMapping("/insert")
	public String insertPage() {
		return "staff/announcement/insertAnnPage";
	}

	@PostMapping("/insert")
	public String insert(@RequestParam String annTitle, @RequestParam String annBegin,
			@RequestParam(required = false) MultipartFile annImg, @RequestParam String annText, Model model) {

		if (annTitle == null || annTitle.trim().isEmpty()) {
			model.addAttribute("errorMsg", "請輸入公告標題");
			return "staff/announcement/index";
		}

		try {
			LocalDateTime begin = LocalDateTime.parse(annBegin);
			byte[] img = (annImg != null && !annImg.isEmpty()) ? annImg.getBytes() : null;

			annService.insertAnn(annTitle.trim(), begin, img, annText);

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("errorMsg", "新增公告失敗");
			return "staff/announcement/index";
		}

		return "redirect:/staff/announcement";
	}

	@GetMapping("/update/{annNo}")
	public String updatePage(@PathVariable Integer annNo, Model model) {
		AnnouncementVO ann = annService.getAnnNoQuery(annNo);
		model.addAttribute("ann", ann);
		return "staff/announcement/updateAnnPage";
	}

	@PostMapping("/update")
	public String update(@RequestParam Integer annNo, @RequestParam String annTitle, @RequestParam String annBegin,
			@RequestParam(required = false) MultipartFile annImg, @RequestParam String annText, Model model) {

		if (annTitle == null || annTitle.trim().isEmpty()) {
			model.addAttribute("errorMsg", "請輸入公告標題");
			return "staff/announcement/index";
		}

		try {
			LocalDateTime begin = LocalDateTime.parse(annBegin);
			byte[] img = (annImg != null && !annImg.isEmpty()) ? annImg.getBytes() : null;

			annService.updateAnn(annNo, annTitle.trim(), begin, img, annText);

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("errorMsg", "修改公告失敗");
			return "staff/announcement/index";
		}

		return "redirect:/staff/announcement";
	}

	@PostMapping("/delete/{annNo}")
	public String delete(@PathVariable Integer annNo) {
		annService.deleteAnn(annNo);
		return "redirect:/staff/announcement";
	}

	@GetMapping("/image/{annNo}")
	public ResponseEntity<byte[]> image(@PathVariable Integer annNo) {
		AnnouncementVO ann = annService.getAnnNoQuery(annNo);
		byte[] img = (ann != null) ? ann.getAnnImg() : null;

		if (img == null || img.length == 0) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).header(HttpHeaders.CACHE_CONTROL, "no-cache")
				.body(img);
	}

}
