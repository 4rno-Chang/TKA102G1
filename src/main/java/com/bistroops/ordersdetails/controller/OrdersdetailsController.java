package com.bistroops.ordersdetails.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.bistroops.ordersdetails.model.OrdersDetailsService;


@Controller
public class OrdersdetailsController {

	@Autowired
	OrdersDetailsService odSvc;
	
//	@GetMapping("/orders")
//	public String OrdersDetails(Model model) {
//		model.addAttribute("ordersDetailslistAll",odSvc.getAll());
//		return "staff/orders";
//	}
	
	
}
