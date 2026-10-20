package com.bistroops.orders.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;
import com.bistroops.ordersdetails.model.OrdersDetailsVO;
import com.bistroops.promote.model.OrderPromoteOptionDTO;
import com.bistroops.promote.model.PromoteService;



@Service
public class OrdersService {
	
	@Autowired
	private OrdersRepository ordersRepository;
	
	@Autowired
	MemberService memberService; 
	
	@Autowired 
	private PromoteService promoteService;

		
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
		
		Integer disc = order.getOrdersPromoteDisc();          // 訂單的折數快照，沒用整單折扣是 null
		if (disc != null) {
			int subtotal = total - discount;                  // 菜品活動價之後的小計（整單折扣「疊加」在菜品活動價之後）
		    int orderDiscount = promoteDiscount(subtotal, disc);
		    order.setOrdersDiscountTotal(discount + orderDiscount);   // 整單折扣併進 orders_discount_total
		    order.setOrdersActualPrice(subtotal - orderDiscount);
		}
	}


	
	//結帳頁左邊重新計算
	@Transactional
	public void recalculateUnpaid(Integer orders) {
		for(OrdersVO o: ordersRepository.findUnpaid()) {
			recalculate(o.getOrders());
		}
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
	
	//============查未結帳狀態================
	public List<OrdersVO> getUnpaid() {
	    return ordersRepository.findUnpaid().stream()  
	    		.filter(o->!"CANCELLED".equals(o.getStatus()))  //不是取消的
	    		.toList();
	}
	public OrdersVO getOne(Integer orders) {
	    return ordersRepository.findById(orders).orElse(null);
	}
	//============查詢workboard訂單=============
	public List<OrdersVO> getActive(){
		return ordersRepository.findActive();
	}
	
	public List<OrdersVO> getDone(){
		return ordersRepository.findUnpaid().stream()
			.filter(o->"DONE".equals(o.getStatus()))
			.toList();
	}
	
	//===========查看會員歷史訂單===========
	
	public List<OrdersVO> getByMemNo(Integer memNo){
		return ordersRepository.findByMemNo(memNo).stream()
				.filter(o->!"CANCELLED".equals(o.getStatus()))  //不是取消的
				.toList();
	}
	

	//===========輸入會員===========
	public String bindMember(Integer order,String phone) {
		if( phone == null|| !phone.matches("09\\d{8}")) {
			return "手機號碼格式錯誤";
		}
		OrdersVO orders =ordersRepository.findById(order).orElse(null);
		if(orders == null)return "找不到此訂單";
		MemberVO member =memberService.getByTel(phone);
		if(member ==null ) {
			return "查無此會員("+phone+")";
		}
		orders.setMember(member);
		ordersRepository.save(orders);
		return null;
		
	}

	//以下六個方法，撰寫by陳巧馨，線上點餐用
	//（整筆取消的訂單不算：由 OrdersRepository 的三個查詢排除，見那裡的【線上點餐・要改】；這兩個方法不用改）
	//查詢訂單是否都結帳，假設訂單都已結帳回傳true
	public boolean isSeatingPaid(String seatNo, LocalDateTime since) {
		int all = ordersRepository.countBySeatSince(seatNo, since);
		int unpaid = ordersRepository.countUnpaidBySeatSince(seatNo, since);
		
		return all > 0 && unpaid == 0;
	}
	
	//已送出訂單按鈕用，同組客人還沒結帳的那張訂單的編號，沒有就是 null
	public Integer getUnpaidOrderNo(String seatNo, LocalDateTime since) {
		List<Integer> orderNos = ordersRepository.findUnpaidNosBySeatSince(seatNo,since);
		
		return orderNos.isEmpty() ? null : orderNos.get(0);
	}
	
	//============整單折扣（結帳時替整張訂單選一個活動打折，一張訂單只能一個）============
	//算整單折扣的金額：recalculate 和 getOrderPromote 都用它，畫面上的金額和實付才會一樣
	private int promoteDiscount(int subtotal, int disc) {
		return (subtotal * (100 - disc) + 50) / 100;   //四捨五入
	}

	//加入（或換成）整單折扣：成功回傳 null，失敗回傳錯誤訊息
	//折數用資料庫查出來的，不相信頁面送來的折數或金額
	@Transactional
	public String applyPromote(Integer orderNo, Integer promoteNo) {
		OrdersVO ordersVO = ordersRepository.findById(orderNo).orElse(null);

		if(ordersVO == null) return "查詢不到訂單";
		if(ordersVO.getOrdersPay() != null) return "此訂單已結帳，不能加入折扣";

		OrderPromoteOptionDTO picked = promoteService.getDiscOptions().stream()
				.filter(o -> o.getPromoteNo().equals(promoteNo)).findFirst().orElse(null);

		if (picked == null) return "這個活動目前不能使用";   // 編號是 null、已結束、被刪除、沒有折數都會找不到

		ordersVO.setPromote(promoteService.getPromoteRef(promoteNo));
		ordersVO.setOrdersPromoteDisc(picked.getPromoteDisc());
		recalculate(orderNo);

		return null;
	}

	//取消整單折扣（取消後不會再被自動加回去，自動套用只在新建訂單那一刻）
	@Transactional
	public String removePromote(Integer orderNo) {
		OrdersVO ordersVO = ordersRepository.findById(orderNo).orElse(null);
		if(ordersVO ==null) return "查詢不到訂單";
		if(ordersVO.getOrdersPay() != null) return "此訂單已結帳，不能取消折扣";

		ordersVO.setPromote(null);
		ordersVO.setOrdersPromoteDisc(null);

		recalculate(orderNo);

		return null;
	}

	//結帳頁「整單折扣」那一行要的資料，沒用整單折扣回傳 null
	public OrderPromoteDTO getOrderPromote(OrdersVO order) {
		if (order == null || order.getOrdersPromoteDisc() == null) return null;

		Integer disc = order.getOrdersPromoteDisc();   //訂單的折數快照，不是活動現在的折數

		//名稱、條件用只查欄位的查詢（讀 order.getPromote() 的名稱會把整筆活動含圖片查出來）
		OrderPromoteOptionDTO info = promoteService.getDiscInfo(order.getPromote().getPromoteNo());
		String name = (info == null) ? "" : info.getPromoteName();
		boolean autoApplied = (info != null && info.getPromoteRule() == null);   //沒有條件＝自動套用的

		//orders_discount_total 已經包含整單折扣，所以菜品的折扣要用明細重新加總（和 recalculate 的迴圈一樣）
		int itemDiscount = 0;
		for (OrdersDetailsVO item : order.getOrdersDetails()) {
			if("已取消".equals(item.getOdStatus()))continue;
			itemDiscount += item.getOdDiscountTotal() == null ? 0 : item.getOdDiscountTotal();
		}
		int subtotal = order.getOrdersTotal() - itemDiscount;

		return new OrderPromoteDTO(name, disc, promoteDiscount(subtotal, disc), autoApplied);
	}
	
	public Map<Integer, OrderPromoteDTO> getOrderPromotes(List<OrdersVO> orders) {
	    Map<Integer, OrderPromoteDTO> result = new HashMap<>();
	    for (OrdersVO o : orders) {
	        OrderPromoteDTO op = getOrderPromote(o);
	        if (op != null) result.put(o.getOrders(), op);
	    }
	    return result;
	}

}
