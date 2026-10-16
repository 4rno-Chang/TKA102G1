package com.bistroops.orderscart.model;

public class CartLineDTO {

	private Integer mealNo;
	private String comment;
	private Integer num;

	public CartLineDTO(Integer mealNo, String comment, Integer num) {
		this.mealNo = mealNo;
		this.comment = comment;
		this.num = num;
	}

	public Integer getMealNo() {
		return mealNo;
	}

	public String getComment() {
		return comment;
	}

	public Integer getNum() {
		return num;
	}
}
