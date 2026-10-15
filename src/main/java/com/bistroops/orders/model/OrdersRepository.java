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
	
	//以下三種方法撰寫by陳巧馨，線上點餐用
	//這一組有多少訂單(已取消之訂單不算)
	@Query("SELECT COUNT(o) FROM OrdersVO o WHERE o.seatNo.seatNo = :seatNo AND o.ordersTime >= :since " +
		   "AND EXISTS (SELECT 1 FROM OrdersDetailsVO d WHERE d.orders = o AND (d.odStatus IS NULL OR d.odStatus <> '已取消'))")
	int countBySeatSince(@Param("seatNo") String seatNo, 
						 @Param("since") LocalDateTime since);
	
	//這一組有幾張「還沒結帳」的訂單(已取消之訂單不算)
	@Query("SELECT COUNT(o) FROM OrdersVO o WHERE o.seatNo.seatNo = :seatNo " +
	       "AND o.ordersTime >= :since AND (o.ordersPay IS NULL OR o.ordersPay = '' ) "+
		   "AND EXISTS (SELECT 1 FROM OrdersDetailsVO d WHERE d.orders = o AND (d.odStatus IS NULL OR d.odStatus <> '已取消'))")
	int countUnpaidBySeatSince(@Param("seatNo") String seatNo, @Param("since") LocalDateTime since);
	
	//這一組「還沒結帳」的訂單編號(已取消之訂單不算)
	@Query("SELECT o.orders FROM OrdersVO o WHERE o.seatNo.seatNo = :seatNo AND o.ordersTime >= :since " +
		   "AND (o.ordersPay IS NULL OR o.ordersPay = '') " +
		   "AND EXISTS (SELECT 1 FROM OrdersDetailsVO d WHERE d.orders = o AND (d.odStatus IS NULL OR d.odStatus <> '已取消')) " +
		   "ORDER BY o.ordersTime")
	List<Integer> findUnpaidNosBySeatSince(@Param("seatNo") String seatNo,
											@Param("since") LocalDateTime since);
	
	
	
	
}
