package com.bistroops.meal.model;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 後台菜單管理：新增／修改菜品對話框送來的欄位（圖片由 Controller 另外用 MultipartFile mealImg 接收）
// 菜名最多 20 字：配合前台菜單和點餐頁的版面
public class MealFormDTO {
	
	private Integer mealNo;
	
	@NotNull(message = "請選擇類別")
	private Integer mealtypeNo;
	
	@Size(max = 30, message = "類別名稱最多 30 個字")
	private String newTypeName;
	
	private String newTypeTag; 
	
	@Size(max = 20, message = "菜名最多 20 個字")
	@NotBlank(message = "請輸入菜名")
	private String mealName;

	@NotNull(message = "請輸入價格")
	@Min(value = 1, message = "價格不能小於 1 元")
	@Max(value = 99999, message = "金額過大請洽詢主管確認情況")
	private Integer mealPrice;
	
	
	@Size(max = 100, message = "說明最多 100 個字")
	private String mealExp;
	
	private String mealStatus;
	
	private boolean removeImg;
	
	private boolean mealPromoOnly;
	
	private List<MealPromoFormDTO> promos;

	public Integer getMealNo() {
		return mealNo;
	}

	public void setMealNo(Integer mealNo) {
		this.mealNo = mealNo;
	}

	public Integer getMealtypeNo() {
		return mealtypeNo;
	}

	public void setMealtypeNo(Integer mealtypeNo) {
		this.mealtypeNo = mealtypeNo;
	}

	public String getNewTypeName() {
		return newTypeName;
	}

	public void setNewTypeName(String newTypeName) {
		this.newTypeName = newTypeName;
	}

	public String getNewTypeTag() {
		return newTypeTag;
	}

	public void setNewTypeTag(String newTypeTag) {
		this.newTypeTag = newTypeTag;
	}

	public String getMealName() {
		return mealName;
	}

	public void setMealName(String mealName) {
		this.mealName = mealName;
	}

	public Integer getMealPrice() {
		return mealPrice;
	}

	public void setMealPrice(Integer mealPrice) {
		this.mealPrice = mealPrice;
	}

	public String getMealExp() {
		return mealExp;
	}

	public void setMealExp(String mealExp) {
		this.mealExp = mealExp;
	}

	public String getMealStatus() {
		return mealStatus;
	}

	public void setMealStatus(String mealStatus) {
		this.mealStatus = mealStatus;
	}

	public boolean isRemoveImg() {
		return removeImg;
	}

	public void setRemoveImg(boolean removeImg) {
		this.removeImg = removeImg;
	}

	public boolean isMealPromoOnly() {
		return mealPromoOnly;
	}

	public void setMealPromoOnly(boolean mealPromoOnly) {
		this.mealPromoOnly = mealPromoOnly;
	}

	public List<MealPromoFormDTO> getPromos() {
		return promos;
	}

	public void setPromos(List<MealPromoFormDTO> promos) {
		this.promos = promos;
	}
	
	
}
