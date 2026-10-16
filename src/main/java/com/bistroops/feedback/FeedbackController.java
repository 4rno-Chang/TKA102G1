package com.bistroops.feedback;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.feedback.model.FeedbackService;
import com.bistroops.feedback.model.FeedbackVO;

@Controller
@RequestMapping("/staff")
public class FeedbackController {
	

	@Autowired
	FeedbackService feedbackService;
	
	private static final int PAGE_SIZE = 10;
	
	
	@GetMapping("/feedback")
	public String feedback(@RequestParam(required = false)Integer orders,
						   @RequestParam(required = false)String tel,
				           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
				           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
						   @RequestParam(required = false)Integer rating,
						   @RequestParam(defaultValue = "1") Integer page,
						   Model model) {
		
		if (page < 1) page = 1;
		// 1. 查出符合條件的總筆數 → 算總頁數
        long totalCount = feedbackService.count(orders, tel, startDate, endDate, rating);
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        
        // 2. 查出這一頁的資料
        List<FeedbackVO> list = feedbackService.search(orders, tel, startDate, endDate, rating, page, PAGE_SIZE);

        // 3. 平均評分（沒資料時給 0，避免 HTML 格式化 null 出錯）
        Double avg = feedbackService.avgRating(orders, tel, startDate, endDate, rating);

        
        
        // 4. 放進 Model → 給 HTML 用
        model.addAttribute("feedbackList", list);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("avgRating", avg == null ? 0.0 : avg);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
		return "staff/feedback";
	}
	@PostMapping("/feedback/delete")
	public String delete(@RequestParam Integer feedbackNo, RedirectAttributes ra) {
	        feedbackService.delete(feedbackNo);
	        ra.addFlashAttribute("successMsg", "已刪除回饋 #" + feedbackNo);
	        return "redirect:/staff/feedback";
	}
	
	
}
