package com.bistroops.feedback.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.feedback.model.FeedbackQrService;
import com.bistroops.feedback.model.FeedbackService;
import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;
import com.bistroops.orders.model.OrdersVO;

import jakarta.servlet.http.HttpSession;

// 前台：會員在「會員中心 → 歷史訂單」按「填寫回饋」或「編輯回饋」送出的表單
// 表單在 templates/front/member/member_modals.html 的 feedbackModal
@Controller
public class FrontFeedbackController {

	@Autowired
	FeedbackService feedbackService;

	@Autowired
	MemberService memberService;
	
	 @Autowired
     FeedbackQrService feedbackQrService;

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
	
    @GetMapping("/bistroops/feedback/qr")
    public String qrPage(@RequestParam(required = false) String token, Model model) {
            model.addAttribute("token", token);
            model.addAttribute("order", feedbackQrService.findOrder(token));
            return "front/feedback/feedback_qr";
    }

    // 客人送出回饋
    @PostMapping("/bistroops/feedback/qr")
    public String qrSubmit(@RequestParam String token,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) String tel,
                           @RequestParam(required = false) Integer rating,
                           @RequestParam(required = false) String content,
                           RedirectAttributes ra) {
            OrdersVO order = feedbackQrService.findOrder(token);
            String error = feedbackService.saveFromQr(order, name, tel, rating, content);
            if (error == null) {
                    feedbackQrService.useToken(token);   // 送出成功，QR Code 作廢
                    ra.addFlashAttribute("done", true);
            } else {
                    ra.addFlashAttribute("errorMsg", error);
            }
            return "redirect:/bistroops/feedback/qr?token=" + token;
    }
}
