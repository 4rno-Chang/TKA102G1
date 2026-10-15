package com.bistroops.frontorder.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bistroops.meal.model.MealEnum;
import com.bistroops.meal.model.MealVO;


public interface MenuRepository extends JpaRepository<MealVO, Integer> {
	
	//不帶圖片查詢
	@Query("SELECT new com.bistroops.frontorder.model.MenuMealCardDTO(" +
		   "m.mealNo, m.mealName, m.mealExp, m.mealPrice, t.mealtypeNo, t.mealtypeName, t.mealtypeHashtag, " +
           "CASE WHEN m.mealImg IS NOT NULL THEN true ELSE false END) " +
           "FROM MealVO m JOIN m.mealType t " +
           "WHERE m.mealStatusEn = :status " +
           "ORDER BY t.mealtypeNo, m.mealNo")
	List<MenuMealCardDTO> findMenuCards(@Param("status") MealEnum status);
	
	//圖片查詢
	@Query("SELECT m.mealImg FROM MealVO m WHERE m.mealNo = :mealNo")
	byte[] findImgByMealNo(@Param("mealNo") Integer mealNo);

}
