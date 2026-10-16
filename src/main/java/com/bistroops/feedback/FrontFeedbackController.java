package com.bistroops.feedback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.feedback.model.FeedbackService;
import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;

import jakarta.servlet.http.HttpSession;

// 前台：會員在「會員中心 → 歷史訂單」按「填寫回饋」或「編輯回饋」送出的表單
// 表單在 templates/front/member/member_modals.html 的 feedbackModal
@Controller
public class FrontFeedbackController {

	@Autowired
	FeedbackService feedbackService;

	@Autowired
	MemberService memberService;

	@PostMapping("/bistroops/feedback/add")
	public String add(@RequestParam(required = false) Integer orders,
	                  @RequestParam(required = false) Integer rating,
	                  @RequestParam(required = false) String content,
	                  @RequestParam(required = false) String returnUrl,
	                  HttpSession session, RedirectAttributes ra) {

		// 送出後回到會員原本所在的前台頁面（和 MemberController 修改會員資料的寫法相同）
		if (returnUrl == null || !returnUrl.startsWith("/bistroops") || returnUrl.startsWith("//")) {
			returnUrl = "/bistroops";
		}

		Integer memNo = (Integer) session.getAttribute("memberNo");
		MemberVO member = memNo == null ? null : memberService.getMemNoQuery(memNo);

		// 沒回饋過就新增，回饋過而且還在 7 天內就修改；回傳的訊息直接顯示成提示
		ra.addFlashAttribute("toastMsg", feedbackService.saveFromMember(member, orders, rating, content));
		// 重新載入後打開會員中心的歷史訂單（member.js 的「7. 頁面載入後」會讀 startTab）
		if (member != null) {
			ra.addFlashAttribute("startTab", "orders");
		}
		return "redirect:" + returnUrl;
	}
}
