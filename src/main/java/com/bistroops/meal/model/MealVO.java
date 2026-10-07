package com.bistroops.meal.model;

import java.util.Arrays;
import java.util.Set;

import com.bistroops.mealtype.model.MealtypeVO;
//import com.bistroops.ordersdetails.model.OrdersDetailsVO;
import com.bistroops.promotemeal.model.PromoteMealVO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;

@Entity
@Table(name = "meal")
public class MealVO  {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "meal_no", updatable = false)
	private Integer mealNo;
	
	@ManyToOne
	@JoinColumn(name = "meal_type_no",referencedColumnName = "meal_type_no")
	private MealtypeVO mealType;
	
	@Column(name = "meal_name",nullable = false)
	private String mealName;
	
	@Column(name = "meal_img", columnDefinition = "mediumblob")
	private byte[] mealImg;
	
	@Column(name = "meal_exp", columnDefinition = "mediumtext")
	private String mealExp;
	
	@Column(name = "meal_price",nullable = false)
	@DecimalMin(value="1",message = "價格不能小於1元")
	private Integer mealPrice;
	
	@Column(name = "meal_status_en", columnDefinition = "enum('上架', '未上架')")
	private MealEnum mealStatusEn;

	@OneToMany(mappedBy="meal",cascade = CascadeType.ALL)
	@OrderBy("meal asc")
	private Set<PromoteMealVO> promoteMeals;
	
//	@OneToMany(mappedBy="ordersDatils")
//	@OrderBy("ordersNo asc")
//	private Set<OrdersDetailsVO> ordersdetails;

	public MealVO() {
	}
	
	

	public Integer getMealNo() {
		return mealNo;
	}

	public void setMealNo(Integer mealNo) {
		this.mealNo = mealNo;
	}

	public MealtypeVO getMealType() {
		return mealType;
	}

	public void setMealType(MealtypeVO mealType) {
		this.mealType = mealType;
	}

	public String getMealName() {
		return mealName;
	}

	public void setMealName(String mealName) {
		this.mealName = mealName;
	}

	public byte[] getMealImg() {
		return mealImg;
	}

	public void setMealImg(byte[] mealImg) {
		this.mealImg = mealImg;
	}

	public String getMealExp() {
		return mealExp;
	}

	public void setMealExp(String mealExp) {
		this.mealExp = mealExp;
	}

	public Integer getMealPrice() {
		return mealPrice;
	}

	public void setMealPrice(Integer mealPrice) {
		this.mealPrice = mealPrice;
	}

	public MealEnum getMealStatusEn() {
		return mealStatusEn;
	}

	public void setMealStatusEn(MealEnum mealStatusEn) {
		this.mealStatusEn = mealStatusEn;
	}

	public Set<PromoteMealVO> getPromoteMeals() {
		return promoteMeals;
	}

	public void setPromoteMeals(Set<PromoteMealVO> promoteMeals) {
		this.promoteMeals = promoteMeals;
	}

//	public Set<OrdersDetailsVO> getOrdersdetails() {
//		return ordersdetails;
//	}
//
//	public void setOrdersdetails(Set<OrdersDetailsVO> ordersdetails) {
//		this.ordersdetails = ordersdetails;
//	}

	@Override
	public String toString() {
		return "MealVO [mealNo=" + mealNo + ", mealType=" + mealType + ", mealName=" + mealName + ", mealImg="
				+ Arrays.toString(mealImg) + ", mealExp=" + mealExp + ", mealPrice=" + mealPrice + ", mealStatusEn="
				+ mealStatusEn + ", promoteMeals=" + promoteMeals + "]";
	}
	
}
