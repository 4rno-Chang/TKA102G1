package com.bistroops.frontorder.model;

import com.bistroops.promote.model.PromoPriceDTO;

public class MenuMealCardDTO {
	
	private Integer mealNo;
	
	private String mealName;
	
	private String mealExp;
	
	private Integer origPrice;
	
	private Integer price;
	
	private String promoName;
	
	private Integer mealtypeNo;

	private String mealtypeName;

	private String tagCode;
	
	private Boolean hasImg;

	public MenuMealCardDTO(Integer mealNo, String mealName, String mealExp, Integer mealPrice,
						   Integer mealtypeNo, String mealtypeName, String tagCode, Boolean hasImg) {
		this.mealNo = mealNo;
		
		this.mealName = mealName;
		
		this.mealExp = mealExp;
		
		this.origPrice = mealPrice;
		
		this.price = mealPrice;
		
		this.mealtypeNo = mealtypeNo;
		
		this.mealtypeName = mealtypeName;
		
		this.tagCode = tagCode;
		
		this.hasImg = hasImg;

		this.promoName = null;
	}
	
	public void applyPromo(PromoPriceDTO now) {
      this.price = now.getPrice();
      this.promoName = now.getPromote().getPromoteName();
	}

	public Integer getMealNo() {
		return mealNo;
	}

	public String getMealName() {
		return mealName;
	}

	public String getMealExp() {
		return mealExp;
	}
	
	public Integer getOrigPrice() {
		return origPrice;
	}

	public Integer getPrice() {
		return price;
	}

	public String getPromoName() {
		return promoName;
	}

	public Integer getMealtypeNo() {
		return mealtypeNo;
	}

	public String getMealtypeName() {
		return mealtypeName;
	}

	public String getTagCode() {
		return tagCode;
	}

	public Boolean getHasImg() {
		return hasImg;
	}

	
}
