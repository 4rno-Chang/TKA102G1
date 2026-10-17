package com.bistroops.mealtype.model;

import java.util.Set;

import com.bistroops.meal.model.MealVO;

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

	@Column(name = "meal_type_hashtag")
	private String mealtypeHashtag;
	
	@OneToMany(mappedBy="mealType")
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

	public String getMealtypeHashtag() {
		return mealtypeHashtag;
	}
	
	public void setMealtypeHashtag(String mealtypeHashtag) {
		this.mealtypeHashtag = mealtypeHashtag;
	}


	public Set<MealVO> getMeals() {
		return meals;
	}

	public void setMeals(Set<MealVO> meals) {
		this.meals = meals;
	}


}
