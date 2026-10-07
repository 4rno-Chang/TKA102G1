package com.bistroops.announcement.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
	
	// 公告及QA 的外殼頁，下方 include 公告列表（tab = 'announcement'）
	@GetMapping({ "", "/list" })
	public String list(Model model) {
		return showList(model);
	}
    
	@GetMapping("/search")
	public String search(@RequestParam(name = "annNo", required = false) String annNoStr, Model model) {
		if (annNoStr == null || annNoStr.trim().isEmpty()) {
			model.addAttribute("errorMsg", "請輸入公告編號");
			return showList(model);
		}

		try {
			Integer annNo = Integer.parseInt(annNoStr.trim());
			AnnouncementVO ann = annService.getAnnNoQuery(annNo);

			if (ann == null) {
				model.addAttribute("errorMsg", "查無此公告編號：" + annNo);
				return showList(model);
			}
			model.addAttribute("ann", ann);
			return "staff/announcement/listOneAnn";

		} catch (NumberFormatException e) {
			model.addAttribute("errorMsg", "公告編號格式錯誤");
			return showList(model);
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
			return showList(model);
		}

		LocalDateTime begin;
		try {
			begin = LocalDateTime.parse(annBegin);
		} catch (Exception e) {
			model.addAttribute("errorMsg", "公告開始時間格式錯誤");
			return showList(model);
		}

		// 不能設定過往日期：帶著使用者剛剛填的內容回到新增頁，顯示紅字
		if (isPast(begin)) {
			model.addAttribute("beginError", "請勿設定過往日期");
			model.addAttribute("annTitle", annTitle);
			model.addAttribute("annBegin", annBegin);
			model.addAttribute("annText", annText);
			return "staff/announcement/insertAnnPage";
		}

		try {
			byte[] img = (annImg != null && !annImg.isEmpty()) ? annImg.getBytes() : null;

			annService.insertAnn(annTitle.trim(), begin, img, annText);

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("errorMsg", "新增公告失敗");
			return showList(model);
		}

		return "redirect:/staff/announcement";
	}

	@GetMapping("/update/{annNo}")
	public String updatePage(@PathVariable Integer annNo, Model model) {
		AnnouncementVO ann = annService.getAnnNoQuery(annNo);
		model.addAttribute("ann", ann);
		model.addAttribute("originalBegin", ann != null ? ann.getAnnBegin() : null);
		return "staff/announcement/updateAnnPage";
	}

	@PostMapping("/update")
	public String update(@RequestParam Integer annNo, @RequestParam String annTitle, @RequestParam String annBegin,
			@RequestParam(required = false) MultipartFile annImg, @RequestParam String annText, Model model) {

		if (annTitle == null || annTitle.trim().isEmpty()) {
			model.addAttribute("errorMsg", "請輸入公告標題");
			return showList(model);
		}

		LocalDateTime begin;
		try {
			begin = LocalDateTime.parse(annBegin);
		} catch (Exception e) {
			model.addAttribute("errorMsg", "公告開始時間格式錯誤");
			return showList(model);
		}

		// 不能改成過往日期；但沒動到原本的時間就放行，不然已經上架的公告會無法修改
		AnnouncementVO original = annService.getAnnNoQuery(annNo);
		if (original == null) {
			model.addAttribute("errorMsg", "查無此公告編號：" + annNo);
			return showList(model);
		}
		boolean unchanged = original.getAnnBegin() != null
				&& original.getAnnBegin().truncatedTo(ChronoUnit.MINUTES).equals(begin);
		if (!unchanged && isPast(begin)) {
			// 另外 new 一個 VO 放使用者剛填的內容，不去改到資料庫查出來的那一筆
			AnnouncementVO ann = new AnnouncementVO();
			ann.setAnnNo(annNo);
			ann.setAnnTitle(annTitle);
			ann.setAnnBegin(begin);
			ann.setAnnImg(original.getAnnImg());
			ann.setAnnText(annText);
			model.addAttribute("ann", ann);
			model.addAttribute("originalBegin", original.getAnnBegin());
			model.addAttribute("beginError", "請勿設定過往日期");
			return "staff/announcement/updateAnnPage";
		}

		try {
			byte[] img = (annImg != null && !annImg.isEmpty()) ? annImg.getBytes() : null;

			annService.updateAnn(annNo, annTitle.trim(), begin, img, annText);

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("errorMsg", "修改公告失敗");
			return showList(model);
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

	// 回到外殼頁的公告分頁；有 errorMsg 的話會顯示在 chip 列上方
	private String showList(Model model) {
		model.addAttribute("annList", annService.getAll());
		model.addAttribute("tab", "announcement");
		return "staff/announcement-and-qa";
	}

	// 表單只到「分」，所以和現在時間比也只比到分，現在這一分鐘還算可以
	private boolean isPast(LocalDateTime begin) {
		return begin.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
	}

}
