package com.bistroops.ordersdetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.orders.model.OrdersService;
import com.bistroops.ordersdetails.model.OrdersDetailsService;

import jakarta.servlet.http.HttpServletRequest;


@Controller
@RequestMapping("staff")
public class OrdersDetailsController {

	@Autowired
	OrdersDetailsService odSvc;
	
	@Autowired
	OrdersService ordersService;
	
	@Autowired
	WorkboardWebSocketHandler workboardWs;// 工作面板的即時通知
	
	//=============修改狀態<input type="hidden" name="itemId" th:value="${item.ordersDetailsNo}">
	@PostMapping("/workboard/toServe")
	public String toServe(@RequestParam Integer itemId) {   //出餐->
		odSvc.updateToserve(itemId);
		workboardWs.notifyChanged();  //通知所有工作面板重新整理
		return "redirect:/staff/workboard?view=kitchen" ;
	}
	//退回上一步 準備中
	@PostMapping("/workboard/backToCooking")
	public String backToCooking(@RequestParam Integer itemId) {
		odSvc.updateTobackToCooking(itemId);
		workboardWs.notifyChanged();  //通知所有工作面板重新整理
		return "redirect:/staff/workboard?view=kitchen";
	}
	
	
	@PostMapping("/workboard/served")
	public String served(@RequestParam Integer itemId) {
		odSvc.updateToserved(itemId);
		workboardWs.notifyChanged();  //通知所有工作面板重新整理
		return "redirect:/staff/workboard?view=floor";
	}
		
//======儲存備註變更

	@PostMapping("/workboard/notes") //orderId、itemIds、itemNotes、cancelledItemIds 
	public String updatenotes(@RequestParam Integer orderId,
							  @RequestParam List<Integer> itemIds,
							  @RequestParam(required = false) List<Integer> cancelledItemIds,
							  HttpServletRequest request,
							  RedirectAttributes ra) {
		
		String[] itemNotes=request.getParameterValues("itemNotes");
		try{
			odSvc.saveNotes(orderId,itemIds,itemNotes,cancelledItemIds);
			ordersService.recalculate(orderId);   // 有取消或復原，金額要重算
			workboardWs.notifyChanged();  //通知所有工作面板重新整理 // 成功才通知
		}catch(IllegalArgumentException e){
			ra.addFlashAttribute("error",e.getMessage());
			return "redirect:/staff/workboard/edit?orderId=" + orderId;
		}
		return "redirect:/staff/workboard?view=kitchen";
	}
		
	//===========整筆訂單取消===========
	@PostMapping("/workboard/cancel")
	public String cancelAll(@RequestParam Integer orderId,
							RedirectAttributes ra) { //錯誤訊息
		try{
			odSvc.cancelAll(orderId);
			ordersService.recalculateUnpaid(orderId); //全部取消金額重新算->歸0
			workboardWs.notifyChanged();  //通知所有工作面板重新整理 // 成功才通知
		}catch(IllegalArgumentException e){
			ra.addFlashAttribute("error",e.getMessage());
			return "redirect:/staff/workboard/edit?orderId=" + orderId;
		}
		return "redirect:/staff/workboard?view=kitchen";
	}
	
	
	
	
	
}
