package com.bistroops.orders.controller;

import java.util.List;

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
	
	
	//===========查看會員歷史訂單===========
	@ModelAttribute("orders")
	public List<OrdersVO> memberOrders(HttpSession session) {
		MemberVO member =(MemberVO) session.getAttribute("member");
		
		if (member == null) {
			return List.of();  // 沒登入給空清單
		}
//		model.addAttribute("orders", ordersService.getByMemNo(member.getMemNo()));
		return ordersService.getByMemNo(member.getMemNo()).stream()
				.filter(o->o.isPaid())  //只顯示已付款的訂單;
				.toList();
	}
	
	
	
	
	
}
