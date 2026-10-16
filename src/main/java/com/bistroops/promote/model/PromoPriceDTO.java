package com.bistroops.promote.model;

//用於打包菜品的活動價
public class PromoPriceDTO {
	private Integer price;
	
	private PromoteVO promote;
	
	public boolean isPromote() { 
		return promote != null;
	}
	
	
	public PromoPriceDTO(Integer price,PromoteVO promote) {
		this.price = price;
		this.promote = promote;
	}

	public Integer getPrice() {
		return price;
	}
	

	public PromoteVO getPromote() {
		return promote;
	}


	
}
