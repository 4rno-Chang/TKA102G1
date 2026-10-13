package com.bistroops.ordersdetails.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersDetailsRepository extends JpaRepository<OrdersDetailsVO, Integer> {

	
	List<OrdersDetailsVO> findByOrders_Orders(Integer orders);//OrdersDetailsVO 裡的 orders 欄位（那張訂單）
}
