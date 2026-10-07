package com.bistroops.promotemeal.model;



import com.bistroops.meal.model.MealVO;
import com.bistroops.promote.model.PromoteVO;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name="promote_meal")
public class PromoteMealVO {
	
	@EmbeddedId
	private ComPromteMealId id;
	
	@MapsId("promoteNo")
	@ManyToOne
	@JoinColumn(name="promote_no", referencedColumnName = "promote_no")
	private PromoteVO promote;
	
	@MapsId("mealNo")
	@ManyToOne
	@JoinColumn(name="meal_no", referencedColumnName = "meal_no")
	private MealVO meal;
	
	@Column(name="promote_discount")
	private Integer promoteDiscount;

	public PromoteMealVO() {
	}
	
	public ComPromteMealId getId() {
	    return id;
	}

	public void setId(ComPromteMealId id) {
	    this.id = id;
	}

	public PromoteVO getPromote() {
		return promote;
	}

	public void setPromote(PromoteVO promote) {
		this.promote = promote;
	}

	public MealVO getMeal() {
		return meal;
	}

	public void setMeal(MealVO meal) {
		this.meal = meal;
	}

	public Integer getPromoteDiscount() {
		return promoteDiscount;
	}

	public void setPromoteDiscount(Integer promoteDiscount) {
		this.promoteDiscount = promoteDiscount;
	}

	@Override
	public String toString() {
	    return "PromoteMealVO [id=" + id + ", promoteDiscount=" + promoteDiscount + "]";
	}
	
	
}
