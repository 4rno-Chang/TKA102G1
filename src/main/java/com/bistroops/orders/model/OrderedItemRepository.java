package com.bistroops.orders.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bistroops.ordersdetails.model.OrdersDetailsVO;

public interface OrderedItemRepository extends JpaRepository<OrdersDetailsVO, Integer> {
	
	//查詢還沒結帳的訂單明細
	@Query("SELECT new com.bistroops.frontorder.model.OrderedItemDTO(" +
		   "d.meal.mealName, d.odMealNum, d.odActualPrice, d.odComment, d.odStatus) " +
		   "FROM OrdersDetailsVO d " +
		   "WHERE d.orders.orders = :ordersNo " +
		   "ORDER BY d.ordersDetailsNo")
	List<OrderedItemDTO> findOrderedItems(@Param("ordersNo") Integer ordersNo);
	
}
