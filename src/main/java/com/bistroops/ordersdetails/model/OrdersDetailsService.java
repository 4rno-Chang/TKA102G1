package com.bistroops.ordersdetails.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrdersDetailsService {

	@Autowired
	private OrdersDetailsRepository repository;
	
	
	
	public List<OrdersDetailsVO> getAll(){
		return repository.findAll();
	}
}
