package com.bistroops.mealtype.model;

import java.util.Set;

import com.bistroops.meal.model.MealVO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "meal_type")
public class MealtypeVO {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "meal_type_no", updatable = false)
	private Integer mealtypeNo;

	@Column(name = "meal_type_name")
	private String mealtypeName;
	
	@OneToMany(mappedBy="mealType",cascade = CascadeType.ALL)
	@OrderBy("mealNo asc")
	private Set<MealVO> meals;


	public MealtypeVO() {
	}

	public Integer getMealtypeNo() {
		return mealtypeNo;
	}



	public void setMealtypeNo(Integer mealtypeNo) {
		this.mealtypeNo = mealtypeNo;
	}



	public String getMealtypeName() {
		return mealtypeName;
	}



	public void setMealtypeName(String mealtypeName) {
		this.mealtypeName = mealtypeName;
	}



	public Set<MealVO> getMeals() {
		return meals;
	}



	public void setMeals(Set<MealVO> meals) {
		this.meals = meals;
	}



	@Override
	public String toString() {
		return "MealtypeVO [mealTypeNo=" + mealtypeNo + ", mealTypeName=" + mealtypeName + ", meals=" + meals + "]";
	}

}
