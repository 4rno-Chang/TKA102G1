package com.bistroops.promotemeal.model;

// 後台菜單管理列表：菜品參加的未結束活動與活動價（price：null＝原價、0＝贈送；started：活動是否已開始）
// 介面映射：getter 對應 PromoteMealRepository.findOpenPromos 查詢裡 AS 的別名
public interface MealPromoDTO {
	Integer getMealNo();
	
	Integer getPromoteNo();
	
	String getPromoteName();
	
	Integer getPrice();
	
	Boolean getStarted();
}
