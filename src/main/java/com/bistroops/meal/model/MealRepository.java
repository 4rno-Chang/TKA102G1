package com.bistroops.meal.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealRepository extends JpaRepository<MealVO, Integer> {

	//後台菜單管理的列表：所有菜品（上架、未上架都要），不帶圖片；類別 → 上架在前、未上架在後 → 價格 → 編號
	@Query("SELECT m.mealNo AS mealNo, m.mealName AS mealName, m.mealExp AS mealExp, m.mealPrice AS mealPrice, " +
           "m.mealStatusEn AS mealStatusEn, t.mealtypeNo AS mealtypeNo, " +
           "CASE WHEN m.mealImg IS NOT NULL THEN true ELSE false END AS hasImg, " +
           "m.mealPromoOnly AS mealPromoOnly " +
           "FROM MealVO m JOIN m.mealType t " +
           "ORDER BY t.mealtypeNo, " +
           "CASE WHEN m.mealStatusEn = :on THEN 0 ELSE 1 END, "+
           "m.mealPrice, m.mealNo")
	List<MealRowDTO> findRows(@Param("on") MealEnum on);
	
	//取消活動限定、上架中，且沒有在進行中的活動菜品
	@Modifying
	@Query("UPDATE MealVO m SET m.mealStatusEn = :off, m.mealPromoOnly = false " +
		   "WHERE m.mealPromoOnly = true AND m.mealStatusEn = :on " +
   		   "AND NOT EXISTS (SELECT pm FROM PromoteMealVO pm WHERE pm.meal = m AND pm.promote.promoteEnd >= :now)")
	int offShelfEndedPromoOnly(@Param("on") MealEnum on, @Param("off") MealEnum off, @Param("now") LocalDateTime now);
	
	//上架活動限定、未上架，有活動要開始的菜品
	@Modifying
	@Query("UPDATE MealVO m SET m.mealStatusEn = :on " +
	       "WHERE m.mealPromoOnly = true AND m.mealStatusEn = :off " +
	       "AND EXISTS (SELECT pm FROM PromoteMealVO pm WHERE pm.meal = m " +
	       "            AND pm.promote.promoteBegin <= :now AND pm.promote.promoteBegin > :dayAgo " +
	       "            AND pm.promote.promoteEnd >= :now)")
	int onShelfStartedPromoOnly(@Param("on") MealEnum on, @Param("off") MealEnum off,
	                            @Param("now") LocalDateTime now, @Param("dayAgo") LocalDateTime dayAgo);
	
	//確認是否有名稱重複
	@Query("SELECT COUNT(m) > 0 FROM MealVO m WHERE m.mealName = :name AND m.mealType IS NOT NULL " +
		   "AND (:mealNo IS NULL OR m.mealNo <> :mealNo)")
	boolean existsActiveName(@Param("name") String name, @Param("mealNo") Integer mealNo);

}
