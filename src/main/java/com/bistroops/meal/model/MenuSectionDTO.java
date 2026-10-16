package com.bistroops.meal.model;

import java.util.ArrayList;
import java.util.List;

public class MenuSectionDTO {
	
	private Integer mealtypeNo;
	
	private String mealtypeName;
	
	private List<MenuMealCardDTO> meals;
		
	public MenuSectionDTO(Integer mealtypeNo, String mealtypeName) {
		this.mealtypeNo = mealtypeNo;
		this.mealtypeName = mealtypeName;
		this.meals = new ArrayList<>();
	}

	public Integer getMealtypeNo() {
		return mealtypeNo;
	}

	public String getMealtypeName() {
		return mealtypeName;
	}

	public List<MenuMealCardDTO> getMeals() {
		return meals;
	}
	
}
