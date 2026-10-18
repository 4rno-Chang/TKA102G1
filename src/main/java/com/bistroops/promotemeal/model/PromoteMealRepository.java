package com.bistroops.promotemeal.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 活動商品（promote_meal）。主鍵是兩個欄位合起來的 ComPromteMealId（活動編號＋菜品編號）
public interface PromoteMealRepository extends JpaRepository<PromoteMealVO, ComPromteMealId> {

	//查詢菜品參加了那些還沒結束的活動
	@Query("SELECT pm.meal.mealNo AS mealNo, p.promoteNo AS promoteNo, p.promoteName AS promoteName, " +
		   "pm.promoteDiscount AS price, " +
		   "CASE WHEN p.promoteBegin <= :now THEN true ELSE false END AS started " +
		   "FROM PromoteMealVO pm JOIN pm.promote p " +
		   "WHERE p.promoteEnd >= :now ORDER BY p.promoteBegin")
	List<MealPromoDTO> findOpenPromos(@Param("now") LocalDateTime now);
	
	//刪除菜品時，把這道菜從「還沒結束」的活動拿掉（這裡是真的刪掉 promote_meal 的資料，已結束的留著當紀錄）
	//（菜品本身不是真的刪，是把類別改成 null，寫在 MealService 的 deleteMeal）
	@Modifying
	@Query("DELETE FROM PromoteMealVO pm WHERE pm.id.mealNo = :mealNo " +
	       "AND pm.id.promoteNo IN (SELECT p.promoteNo FROM PromoteVO p WHERE p.promoteEnd >= :now)")
	int deleteOpenByMeal(@Param("mealNo") Integer mealNo, @Param("now") LocalDateTime now);

	// 這個活動的菜品中，同時參加了時間重疊（跟 begin～end 重疊）的其他活動的菜名
	@Query("SELECT DISTINCT a.meal.mealName FROM PromoteMealVO a, PromoteMealVO b " +
		   "WHERE a.promote.promoteNo = :promoteNo AND b.meal = a.meal AND b.promote.promoteNo <> :promoteNo " +
		   "AND a.meal.mealType IS NOT NULL " +
		   "AND b.promote.promoteBegin <= :end AND b.promote.promoteEnd >= :begin")
	List<String> findOverlapMealNames(@Param("promoteNo") Integer promoteNo,
			                          @Param("begin") LocalDateTime begin, 
			                          @Param("end") LocalDateTime end);
	
}
