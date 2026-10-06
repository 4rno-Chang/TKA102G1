package com.bistroops.seattype.model;

import java.util.List;


public interface SeatType_interface {

	 public void insert(SeatTypeVO seatTypeVO);  //取得整筆物件
     public void update(SeatTypeVO seatTypeVO);
     public void delete(String seatTypeno); //取得PK
     public SeatTypeVO findByPrimaryKey(String seatTypeNo);//單筆查詢
     public List<SeatTypeVO> getAll(); //查詢全部物件
}
