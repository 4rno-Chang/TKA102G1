package com.bistroops.ordersdetails.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdersDetailsService {

	@Autowired
	private OrdersDetailsRepository repository;
	
	
	
	public List<OrdersDetailsVO> getAll(){
		return repository.findAll();
	}
	
	//=============== 製作中->等待送餐 ==================
		@Transactional  //沒有交易的話，Hibernate 不會把改動寫回資料庫，所以按了按鈕、頁面重新整理後，狀態還是原來的
	public void updateToserve(Integer ordersDetailsNo){
		OrdersDetailsVO item=repository.findById(ordersDetailsNo).orElseThrow();
		if("製作中".equals(item.getOdStatus())) {
			item.setOdStatus("等待送餐");
		};
	}
	//=============== 退回上一步 等待送餐->製作中 =================
	@Transactional
	public void updateTobackToCooking(Integer ordersDetailsNo){
		OrdersDetailsVO item=repository.findById(ordersDetailsNo).orElseThrow();
		if("等待送餐".equals(item.getOdStatus())) {
			item.setOdStatus("製作中");
		};
	}
	//=============== 等待送餐->已送達 =================
	@Transactional
	public void updateToserved(Integer ordersDetailsNo){
		OrdersDetailsVO item=repository.findById(ordersDetailsNo).orElseThrow();
		if("等待送餐".equals(item.getOdStatus())) {
			item.setOdStatus("已送達");
			item.setOdServedAt(LocalDateTime.now());
		};
	}
	
	@Transactional
	public void saveNotes(Integer orderId,List<Integer> itemIds,String[] itemNotes,List<Integer> cancelledItemIds){
		List<Integer> cancelled=( cancelledItemIds==null)?List.of():cancelledItemIds;   //是 null → 用 List.of(),一個空的不可變 List
		
		if (cancelled.containsAll(itemIds)) {
		        throw new IllegalArgumentException("不能刪除全部餐點，請使用刪除訂單");
		}
		for (int i = 0; i < itemIds.size(); i++) {
	        OrdersDetailsVO item = repository.findById(itemIds.get(i)).orElseThrow();
	        if (!item.getOrders().getOrders().equals(orderId)) continue;   // 不是這張訂單的餐點，不動

	        if (itemNotes != null && i < itemNotes.length) {
	            item.setOdComment(itemNotes[i]);
	        }

	        boolean wantCancel = cancelled.contains(item.getOrdersDetailsNo());
	        String now = item.getOdStatus();
	        if (wantCancel && !"已送達".equals(now)) {
		        if(item.getOrders().isPaid()) {  // 從任一筆明細往回找訂單get(0)（都是同一張），問它付款了沒
					throw new IllegalArgumentException("此訂單已結帳，無法刪除這道菜");
				}
	            item.setOdStatus("已取消");
	        } else if (!wantCancel && "已取消".equals(now)) {
	            item.setOdStatus("製作中");   // 復原
	        }
	    }
	}
	//===========整筆訂單取消===========
	@Transactional  //如果沒有交易 = 撈完就脫離管理
	public void cancelAll(Integer orderId) {
		List<OrdersDetailsVO> items =repository.findByOrders_Orders(orderId);
		if (items.isEmpty())return;
		
		if(items.get(0).getOrders().isPaid()) {  // 從任一筆明細往回找訂單get(0)（都是同一張），問它付款了沒
			throw new IllegalArgumentException("此訂單已結帳，不能刪除");
		}
		
		for(OrdersDetailsVO item:items) {  
			String now = item.getOdStatus();
			if(!"製作中".equals(now) &&!"已取消".equals(now)) {  // 不是製作中 也不是已取消 → 代表已送達或等待送餐
				throw new IllegalArgumentException("已有餐點出餐，不能刪除整張訂單");
        	}
		}
		for (OrdersDetailsVO item : items) {
	        item.setOdStatus("已取消");//方法結束，交易 commit
	    }
	}
	
}
