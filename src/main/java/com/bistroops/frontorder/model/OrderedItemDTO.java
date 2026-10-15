package com.bistroops.frontorder.model;

public class OrderedItemDTO {
	private String mealName;
	
	private Integer num;
	
	private Integer subtotal;

	private String comment;

	private String status;
	
	public boolean isCancelled() { 
		return "已取消".equals(status); 
	}

	public OrderedItemDTO(String mealName, Integer num, Integer subtotal, String comment, String status) {
		super();
		this.mealName = mealName;
		this.num = num;
		this.subtotal = subtotal;
		this.comment = comment;
		this.status = status;
	}

	public String getMealName() {
		return mealName;
	}

	public Integer getNum() {
		return num;
	}

	public Integer getSubtotal() {
		return subtotal;
	}

	public String getComment() {
		return comment;
	}

	public String getStatus() {
		return status;
	}

}
