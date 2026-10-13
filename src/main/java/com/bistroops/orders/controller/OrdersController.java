package com.bistroops.orders.controller;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bistroops.orders.model.OrdersService;
import com.bistroops.orders.model.OrdersVO;
import com.bistroops.ordersdetails.model.OrdersDetailsService;

@Controller
@RequestMapping("/staff")
public class OrdersController {

	@Autowired
	OrdersService ordersService;
	
	@Autowired
	OrdersDetailsService ordersDetailsService;
	
	
	
	@GetMapping("/orders")
	public String orders(@RequestParam(required =false)String status, //目前狀態
						 @RequestParam(required =false)String keyword, //搜尋關鍵字
						 @RequestParam(required =false)String date, // 日期
						 Model model) {
		LocalDate today=LocalDate.now();
		LocalDate day= today ;
		try{ 
			if (date!=null && !date.isBlank())  
				day = LocalDate.parse(date);  //格式判斷：parse 失敗會丟例外
		}catch(DateTimeParseException e){
			day = today;     // 格式錯就用今天
		}
		if (day.isAfter(today)) //不准選未來
			day=today; 
		
		String weekday ="一二三四五六日".substring(day.getDayOfWeek().getValue()-1,  //返回一個列舉（Enum）型態。
				day.getDayOfWeek().getValue());
		model.addAttribute("status",status == null ?"" : status);
		model.addAttribute("keyword",keyword);
		model.addAttribute("date",day);
		model.addAttribute("dateText",day.format(DateTimeFormatter.ofPattern("MM/dd"))+"("+ weekday +")");
		model.addAttribute("prevDate",day.minusDays(1));//減一天
		model.addAttribute("nextDate",day.isBefore(today)?day.plusDays(1):null);//加一天plusDays,今天的話給 null,前端就不顯示「下一天」按鈕
		model.addAttribute("today",today);
		
		model.addAttribute("orderslistAll", ordersService.search(status, keyword, day));//查資料庫
		return "staff/orders";
	}
	
	//===========結帳===============
		@GetMapping("/checkout") //staff/checkout(order=${o.orders})
		public String checkout(@RequestParam(required = false) Integer order, Model model) {
			if(order != null)ordersService.recalculateUnpaid(order);//一進頁面就重新計算總金額
		    model.addAttribute("checkoutListout", ordersService.getUnpaid());
		    if (order != null) model.addAttribute("order", ordersService.getOne(order));
		    return "staff/checkout";
		}
		@PostMapping("/checkout/pay")
		public String pay(@RequestParam Integer order,
						  @RequestParam String method) {
			ordersService.pay(order,method);
			return "redirect:/staff/checkout";
		}
		
		
		//==============workboard給畫面==================
		@GetMapping("/workboard") //@{/staff/workboard(view='kitchen')}
		public String workboard(@RequestParam(defaultValue="kitchen")String view,Model model) {//view=kitchen、floor、done
			model.addAttribute("view",view); 
			model.addAttribute("workboardlistAll", "done".equals(view)?ordersService.getDone():ordersService.getActive());
			
			return "staff/workboard" ;
		}
		
		//============workboard訂單修改=============
		@GetMapping("/workboard/edit")
		public String workboardEdit(@RequestParam Integer orderId,Model model) {
			OrdersVO editOrder=ordersService.getOne(orderId);
			if (editOrder == null) return "redirect:/staff/workboard";
			
			model.addAttribute("view","kitchen");
			model.addAttribute("workboardlistAll",ordersService.getActive());
			
			model.addAttribute("editOrder",editOrder); //取得明細
			model.addAttribute("canCancel", editOrder.getOrdersDetails().stream()
			            .allMatch(d -> "製作中".equals(d.getOdStatus()) || "已取消".equals(d.getOdStatus())));
			
			return "/staff/workboard";
		}
	
	
	
}
