package com.bistroops.promote.controller;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.promote.model.PromoteService;
import com.bistroops.promote.model.PromoteVO;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/staff")
public class PromoteController {

	@Autowired
	PromoteService promoteSve;
	
	@GetMapping("/promote")
	public String promote(@RequestParam(required = false) String status,
						  @RequestParam(required = false) String keyword,
			              @RequestParam(required = false) Integer page,
			              Model model) {
		if (page == null || page < 1) page = 1;
		
		LocalDateTime now = LocalDateTime.now();
		Page<PromoteVO> result=promoteSve.getPromoteByStatus(status, keyword, now, page);
		
		int totalPages = Math.max(1, result.getTotalPages());
		
		if (page > totalPages) {
	        page = totalPages;
	        result = promoteSve.getPromoteByStatus(status, keyword, now, page);
	    }
		
		model.addAttribute("promotes", result.getContent());
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("total", result.getTotalElements());
		model.addAttribute("page", page);
		model.addAttribute("status", status);
		model.addAttribute("keyword", keyword);
		
		return "staff/promote/promote_list";
	}
	
	@GetMapping("/promote/detail")
	public String promoteDetail(@RequestParam(required = false) String status,
								@RequestParam(required = false) String keyword,
								@RequestParam(required = false) Integer page,
								@RequestParam Integer promoteNo,
								Model model) {
		if (page == null || page < 1) page = 1;
		
		LocalDateTime now = LocalDateTime.now();
		Page<PromoteVO> result=promoteSve.getPromoteByStatus(status, keyword, now, page);
		
		int totalPages = Math.max(1, result.getTotalPages());
		
		if (page > totalPages) {
	        page = totalPages;
	        result = promoteSve.getPromoteByStatus(status, keyword, now, page);
	    }
		
		model.addAttribute("promotes", result.getContent());
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("total", result.getTotalElements());
		model.addAttribute("page", page);
		model.addAttribute("status", status);
		model.addAttribute("keyword", keyword);
		
		PromoteVO promoteDetail = promoteSve.getPromoteByNo(promoteNo);
		model.addAttribute("promoteDetail", promoteDetail);
		
		if (promoteDetail == null) {
			return "redirect:/staff/promote";
		}		
		return "staff/promote/promote_list";
	}
	
	@GetMapping("/promote/add")
	public String addPromote(@RequestParam(required = false) String pick,
							 @RequestParam(required = false) String pickKeyword,
            				 @RequestParam(required = false) Integer copyFrom,
            				 Model model) {
		PromoteVO promoteVO = new PromoteVO();
		
		PromoteVO pastPromote = promoteSve.getPromoteByNo(copyFrom);
		if ( pastPromote != null && promoteSve.getStatus(pastPromote).equals("已結束")) {
			promoteVO.setPromoteName(pastPromote.getPromoteName());
			promoteVO.setPromoteContent(pastPromote.getPromoteContent());
		}
		model.addAttribute("promoteForm", promoteVO);
		
		if ( pick != null ) {
			Page<PromoteVO> result = promoteSve.getPromoteByEnd(pickKeyword, LocalDateTime.now(), 1);
	        model.addAttribute("pickList", result.getContent());
	        model.addAttribute("pickKeyword", pickKeyword);
		}
		
		return "staff/promote/promote_form";
	}
	
	@GetMapping("/promote/edit")
	public String editPromote(@RequestParam Integer promoteNo, 
						Model model, RedirectAttributes redirectAttributes) {
		PromoteVO promoteForm = promoteSve.getPromoteByNo(promoteNo);
		
		if (promoteForm == null) {
			redirectAttributes.addFlashAttribute("message", "查無此活動");
			return "redirect:/staff/promote";
		}
		
		if (promoteSve.getStatus(promoteForm).equals("已結束")) {
			redirectAttributes.addFlashAttribute("message", "已結束的活動不能修改");
			return "redirect:/staff/promote";
		}
		
		model.addAttribute("promoteForm", promoteForm);
		return "staff/promote/promote_form";
	}
	
	@PostMapping("/promote/save")
	public String UpdatePromote(@Valid @ModelAttribute("promoteForm")PromoteVO promoteVO,BindingResult result,
						 @RequestParam(value = "upImg", required = false) MultipartFile upImg,
						 Model model,
						 RedirectAttributes redirectAttributes) throws IOException {
		if (result.hasErrors()) {
			FieldError error = result.getFieldErrors().get(0);
			String text;
			if("typeMismatch".equals(error.getCode())) {
				text = "日期的格式不正確，請重新選擇";
			} else {
				text = error.getDefaultMessage();
			}
			
			model.addAttribute("errorMsg", text);
			return "staff/promote/promote_form";
		}
		
		// 2. 查出資料庫原本那一筆
		PromoteVO originalPromote = null;
		if (promoteVO.getPromoteNo() != null) {
			originalPromote = promoteSve.getPromoteByNo(promoteVO.getPromoteNo());
			if (originalPromote == null || promoteSve.getStatus(originalPromote).equals("已結束")) {
				redirectAttributes.addFlashAttribute("message", "查無此活動或已結束的活動不能修改");
				return "redirect:/staff/promote";
			}
		}
		
		// 3. 日期檢查
		String dateErrorMsg;
		if (originalPromote == null) {
		    dateErrorMsg = promoteSve.checkNotStartPromoteDate(promoteVO);
		} else if(promoteSve.getStatus(originalPromote).equals("未開始")) {
			dateErrorMsg = promoteSve.checkNotStartPromoteDate(promoteVO);
		} else { 
			dateErrorMsg = promoteSve.checkActivePromoteDate(promoteVO, originalPromote);
		}
		
		if (dateErrorMsg != null) {
		    if (originalPromote != null) {
		        promoteVO.setPromoteImg(originalPromote.getPromoteImg());
		    }
		    model.addAttribute("errorMsg", dateErrorMsg);
		    return "staff/promote/promote_form";
		}
		
		//4. 圖片
		if(upImg != null && !upImg.isEmpty()) {
			promoteVO.setPromoteImg(upImg.getBytes());
		}
		
		// 5. 存檔
		if (originalPromote == null) {
			promoteSve.addPromote(promoteVO,promoteVO.getPromoteImg());
			redirectAttributes.addFlashAttribute("message", "已新增活動");
		}
		else {
			PromoteVO update = promoteSve.updatePromote(
					promoteVO.getPromoteNo(), promoteVO.getPromoteName(), promoteVO.getPromoteContent(),
					promoteVO.getPromoteBegin(), promoteVO.getPromoteEnd(), promoteVO.getPromoteImg());
			if ( update == null ) {
				redirectAttributes.addFlashAttribute("message", "修改活動失敗請重新輸入");				
			} else {
			redirectAttributes.addFlashAttribute("message", "已修改活動");
			}
		
		}
		return "redirect:/staff/promote";
	}
	
	
	@GetMapping("/promote/img")
	public ResponseEntity<byte[]> promoteImg(@RequestParam Integer promoteNo) {
		PromoteVO promoteVO = promoteSve.getPromoteByNo(promoteNo);
		
		byte[] img = (promoteVO != null) ? promoteVO.getPromoteImg() : null;
		
		if( img == null || img.length == 0) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")   
                .body(img);
	}
	
	@PostMapping("/promote/delete")
	public String deletePromote(@RequestParam (required = false) Integer promoteNo, 
						  RedirectAttributes redirectAttributes) {

			String text = promoteSve.deleteNotStartPromote(promoteNo);
			redirectAttributes.addFlashAttribute("message", text);
			return "redirect:/staff/promote";
	}
	
	
	
	
	
}
