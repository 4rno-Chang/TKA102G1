package com.bistroops.orders.model;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.ordersdetails.model.OrdersDetailsVO;



@Service
public class OrdersService {
	
	@Autowired
	private OrdersRepository ordersRepository;
	
		
	//查詢全部
	public List<OrdersVO> getAll(){
		return ordersRepository.findAll();
	}
	// 依照訂單時間 取得新到舊
//	public List<OrdersVO> findByOrdersTime() {
//	    List<OrdersVO> list = ordersRepository.findAll();
//	    list.sort(Comparator.comparing(OrdersVO::getOrdersTime).reversed());  
//	    return list;
//	}
	
	//查某一天（自訂）
	public List<OrdersVO> search(String status,String keyword,LocalDate day){
		String kw =(keyword==null) ?"" :keyword.trim().toLowerCase();
		return ordersRepository.findByDay(day.atStartOfDay(), day.plusDays(1).atStartOfDay())
				.stream()
				.filter(o->status==null ||status.isEmpty()||status.equals(o.getStatus()))
				.filter(o->kw.isEmpty()
						||o.getSeatNo().getSeatNo().toLowerCase().contains(kw)
						||String.valueOf(o.getOrders()).equals(kw))
				.toList();
				
	}
	
	//重新計算訂單實付金額
	@Transactional
	public void recalculate(Integer orders) { 
		OrdersVO order=ordersRepository.findById(orders).orElseThrow();
		
		int total=0, discount=0;
		for (OrdersDetailsVO item : order.getOrdersDetails()) {
			if("已取消".equals(item.getOdStatus()))continue;
			
			
			
			int itemDiscount = item.getOdDiscountTotal() == null ? 0 :item.getOdDiscountTotal();  //取得折扣總額
			int itemTotal =item.getOdDiscountPrice() * item.getOdMealNum(); //下單時菜品金額x數量   
			
			item.setOdActualPrice(itemTotal-itemDiscount);
			total+=itemTotal;
			discount+=itemDiscount;
		}
		order.setOrdersTotal(total);  //訂單總金額
		
		order.setOrdersDiscountTotal(discount);  //訂單折扣金額
		order.setOrdersActualPrice(total-discount);    //實付金額
		
	}
	//修改訂單付款方式
	@Transactional
	public void pay(Integer order,String method) { 
		if(!method.equals("現金") && !method.equals("刷卡")) {
			throw new IllegalArgumentException("付款方式錯誤"); //非法參數異常
		}
		OrdersVO orders =ordersRepository.findById(order).orElseThrow();
		if(orders.getOrdersPay()!=null && !orders.getOrdersPay().isEmpty()) {
			throw new IllegalArgumentException("此筆訂單已結過帳");
		}
	    recalculate(order);//重新計算總金額
		orders.setOrdersPay(method);
		ordersRepository.save(orders);  	
	}
	
	
	
	
	
	
	//查未結帳狀態
	public List<OrdersVO> getUnpaid() {
	    return ordersRepository.findUnpaid().stream()  
	    		.filter(o->!"CANCELLED".equals(o.getStatus()))  //不是取消的
	    		.toList();
	}
	public OrdersVO getOne(Integer orders) {
	    return ordersRepository.findById(orders).orElse(null);
	}

	
	
	
	
}
