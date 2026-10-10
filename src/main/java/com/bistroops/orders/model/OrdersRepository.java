package com.bistroops.orders.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdersRepository extends JpaRepository<OrdersVO,Integer>{
	

//	@Query("SELECT DISTINCT o FROM OrdersVO o LEFT JOIN FETCH o.ordersDetails")
//	List<OrdersVO> findAllWithDetails();
	
	
	
	//查某一天
	@Query ("SELECT DISTINCT o FROM OrdersVO o LEFT JOIN FETCH o.ordersDetails " +
	"WHERE o.ordersTime >= :start AND o.ordersTime < :end ORDER BY o.ordersTime DESC")
	List<OrdersVO> findByDay(@Param("start")LocalDateTime start, @Param("end")LocalDateTime end);
	
	
	//查未結帳
	@Query("SELECT DISTINCT o FROM OrdersVO o LEFT JOIN FETCH o.ordersDetails " + 
				"WHERE o.ordersPay IS NULL OR o.ordersPay = '' ORDER BY o.ordersTime")
	List<OrdersVO> findUnpaid(); 

	
}
