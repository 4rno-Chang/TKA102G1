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

	//查廚房工作看板
	@Query ("SELECT DISTINCT o FROM OrdersVO o "
			+ "LEFT JOIN FETCH o.ordersDetails d "
			+ "LEFT JOIN FETCH d.meal m "
			+ "LEFT JOIN FETCH m.mealType t "
			+ "WHERE EXISTS (SELECT 1 FROM OrdersDetailsVO x " + 
			"WHERE x.orders = o AND x.odStatus NOT IN ('已送達', '已取消')) " +  //條件：這張訂單「至少有一筆」還沒送達也沒取消→ 也就是廚房還有事要做的訂單
		       "ORDER BY o.ordersTime, t.mealtypeNo, d.ordersDetailsNo")
	List<OrdersVO>findActive();
	
	
	//===========查看會員歷史訂單===========
	@Query("SELECT DISTINCT o FROM OrdersVO o LEFT JOIN FETCH o.ordersDetails " + 
			"WHERE o.member.memNo = :memNo ORDER BY o.ordersTime DESC")  //:memNo 是具名參數的佔位符,代表「這裡的值等執行時再填」。
	public List<OrdersVO> findByMemNo(@Param("memNo") Integer memNo);
	
	
	
	
	
	
}
