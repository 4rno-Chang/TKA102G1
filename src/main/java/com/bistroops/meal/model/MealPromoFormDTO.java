package com.bistroops.meal.model;

// 後台菜單管理：對話框送來的一個活動的活動價（MealFormDTO.promos 的一筆；表單欄位 promos[i].promoteNo／joined／price）
public class MealPromoFormDTO {
	
	private Integer promoteNo ;
	
	private boolean joined;
	
	private Integer price;

	public Integer getPromoteNo() {
		return promoteNo;
	}

	public void setPromoteNo(Integer promoteNo) {
		this.promoteNo = promoteNo;
	}

	public boolean isJoined() {
		return joined;
	}

	public void setJoined(boolean joined) {
		this.joined = joined;
	}

	public Integer getPrice() {
		return price;
	}

	public void setPrice(Integer price) {
		this.price = price;
	}
	

}
