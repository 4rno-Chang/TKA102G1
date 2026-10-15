package com.bistroops.frontorder.model;


public class CartItemDTO {
	
	private Integer mealNo;
	
	private String mealName;
	
	private Integer price; 
	
	private Integer num;
	
	private String comment;
	
	private Integer subtotal;

	public Integer getMealNo() {
		return mealNo;
	}

	public CartItemDTO(Integer mealNo, String mealName, Integer price, Integer num, String comment) {
		this.mealNo = mealNo;
		this.mealName = mealName;
		this.price = price;
		this.num = num;
		this.comment = comment;
		this.subtotal = (price != null && num != null) ? price * num : 0;
	}

	public void setMealNo(Integer mealNo) {
		this.mealNo = mealNo;
	}

	public String getMealName() {
		return mealName;
	}

	public Integer getPrice() {
		return price;
	}

	public Integer getNum() {
		return num;
	}

	public String getComment() {
		return comment;
	}

	public Integer getSubtotal() {
		return subtotal;
	}
	
}
