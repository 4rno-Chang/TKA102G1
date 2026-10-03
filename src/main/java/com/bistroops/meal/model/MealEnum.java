package com.bistroops.meal.model;

public enum MealEnum {
	ONSALE("上架"), NOTSALE("未上架");

	private String mealStatusEn;

	MealEnum(String mealStatusEn) {
		this.mealStatusEn = mealStatusEn;
	}

	public String getMealStatusEn() {
		return mealStatusEn;
	}
	
	public void setMealEnum(String mealStatusEn) {
		this.mealStatusEn = mealStatusEn;
	}
	
	
	public static MealEnum fromMealEnum(String mealStatusEn) {
		if (mealStatusEn == null) {
			return null;
		}

		for (MealEnum status : MealEnum.values()) {
			if (status.mealStatusEn.equals(mealStatusEn)) {
				return status;
			}
		}
		return null;
	}
}
