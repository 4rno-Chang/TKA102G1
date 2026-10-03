package com.bistroops.promotemeal.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ComPromteMealId implements Serializable{
	private static final long serialVersionUID = 1L;

	@Column(name = "promote_no")
	private Integer promoteNo;
	
	@Column(name = "meal_no")
	private Integer mealNo;

	public ComPromteMealId() {
	}
	
	public ComPromteMealId(Integer promoteNo, Integer mealNo) {
		super();
		this.promoteNo = promoteNo;
		this.mealNo = mealNo;
	}
	
	public Integer getPromoteNo() {
		return promoteNo;
	}
	
	public void setPromoteNo(Integer promoteNo) {
		this.promoteNo = promoteNo;
	}
	
	public Integer getMealNo() {
		return mealNo;
	}
	
	public void setMealNo(Integer mealNo) {
		this.mealNo = mealNo;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(mealNo, promoteNo);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComPromteMealId other = (ComPromteMealId) obj;
		return Objects.equals(promoteNo, other.promoteNo) && Objects.equals(mealNo, other.mealNo);
	}
}
