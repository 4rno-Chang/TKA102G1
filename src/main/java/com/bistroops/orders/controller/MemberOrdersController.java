package com.bistroops.orders.controller;

import java.util.List;

import com.bistroops.member.model.MemberService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bistroops.member.model.MemberVO;
import com.bistroops.orders.model.OrdersService;
import com.bistroops.orders.model.OrdersVO;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice//讓所有頁面的 Model 自動帶 orders。
@RequestMapping("/front")
public class MemberOrdersController {
	
	

	@Autowired
	OrdersService ordersService;
	
	@Autowired
	private MemberService memberService;

	@ModelAttribute("member")
	public MemberVO currentMember(HttpSession session) {
	    Integer memNo = (Integer) session.getAttribute("memberNo");

	    if (memNo == null) {
	        return null;
	    }

	    return memberService.getMemNoQuery(memNo);
	}
	
	
	//===========查看會員歷史訂單===========
	@ModelAttribute("orders")
	public List<OrdersVO> memberOrders(HttpSession session) {
		// 從 Session 取得目前登入的會員編號
		Integer memNo = (Integer) session.getAttribute("memberNo");

		if (memNo == null) {
		    return List.of();
		}
//		model.addAttribute("orders", ordersService.getByMemNo(member.getMemNo()));
		return ordersService.getByMemNo(memNo).stream()
		        .filter(o -> o.isPaid())
		        .toList();
	}
	
	
	
	
	
}
