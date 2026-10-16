package com.bistroops.orders.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.bistroops.IndexController_inSpringBoot;
import com.bistroops.announcement.controller.FrontAnnouncementController;
import com.bistroops.feedback.model.FeedbackService;
import com.bistroops.feedback.model.FeedbackVO;
import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;
import com.bistroops.orders.model.OrdersService;
import com.bistroops.orders.model.OrdersVO;
import com.bistroops.promote.controller.FrontPromoteController;
import com.bistroops.qa.controller.FrontQaController;
import com.bistroops.reservation.controller.ReservationFrontController;
import com.bistroops.waiting.controller.WaitingController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice(assignableTypes = {  // 只對這些 Controller 生效
        IndexController_inSpringBoot.class,
        FrontAnnouncementController.class,
        FrontOrderController.class,
        FrontPromoteController.class,
        FrontQaController.class,
        ReservationFrontController.class,
        WaitingController.class
})
public class MemberOrdersController {
	
	

	@Autowired
	OrdersService ordersService;
	
	@Autowired
	private MemberService memberService;

	@Autowired
	private FeedbackService feedbackService;

	@ModelAttribute("member")
	public MemberVO currentMember(HttpServletRequest  request) {
		HttpSession session =request.getSession(false); // 沒有 session 就不要新建（不然會蓋掉員工的登入 cookie）
	    Integer memNo = session==null ?null  :(Integer) session.getAttribute("memberNo");

	    if (memNo == null) {
	        return null;
	    }

	    return memberService.getMemNoQuery(memNo);
	}
	
	
	//===========查看會員歷史訂單===========
	@ModelAttribute("orders")
	public List<OrdersVO> memberOrders(HttpServletRequest request) {
		 // 從 Session 取得目前登入的會員編號；沒有 session 就不要新建
		HttpSession session = request.getSession(false);
		Integer memNo =session==null ?null  :(Integer) session.getAttribute("memberNo");

		if (memNo == null) {
		    return List.of();
		}
//		model.addAttribute("orders", ordersService.getByMemNo(member.getMemNo()));
		return ordersService.getByMemNo(memNo).stream()
		        .filter(o -> o.isPaid())
		        .toList();
	}

	//===========歷史訂單的回饋（訂單編號 → 回饋）===========
	// member_modals.html 用它決定每筆訂單顯示「填寫回饋」「編輯回饋」還是「已回饋」
	@ModelAttribute("feedbackMap")
	public Map<Integer, FeedbackVO> memberFeedbacks(HttpServletRequest request) {
		return feedbackService.getMapByOrders(memberOrders(request));
	}
	
	
	
	
	
}
