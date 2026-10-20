package com.bistroops.orders.model;

// 結帳頁「整單折扣」那一行要顯示的資料（OrdersService.getOrderPromote 產生，Model 名稱是 orderPromote）
//   promoteDisc 是訂單的折數快照；discountAmount 和 recalculate 用同一個公式算（OrdersService.promoteDiscount）
//   autoApplied：活動沒有條件 → true，畫面會顯示「活動無條件限制自動套用」
public class OrderPromoteDTO {
	
	private String  promoteName;
	
	private Integer promoteDisc;
	
	private Integer discountAmount;
	
	private boolean autoApplied;
	
	

	public OrderPromoteDTO(String promoteName, Integer promoteDisc, 
						   Integer discountAmount, boolean autoApplied) {
		this.promoteName = promoteName;
		this.promoteDisc = promoteDisc;
		this.discountAmount = discountAmount;
		this.autoApplied = autoApplied;
	}

	public String getPromoteName() {
		return promoteName;
	}

	public Integer getPromoteDisc() {
		return promoteDisc;
	}


	public Integer getDiscountAmount() {
		return discountAmount;
	}


	public boolean isAutoApplied() {
		return autoApplied;
	}

	

}
