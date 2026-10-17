package com.bistroops.meal.model;

public interface MealRowDTO {

	Integer getMealNo();
  
	String getMealName();
  
	String getMealExp();
  
	Integer getMealPrice();
  
	MealEnum getMealStatusEn();
  
	Integer getMealtypeNo();
  
	Boolean getHasImg();
  
	Boolean getMealPromoOnly();
  
	default String getStatus() {
		return (getMealStatusEn() == MealEnum.ONSALE) ? "上架" : "未上架";
	}

}
