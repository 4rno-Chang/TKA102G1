package com.bistroops.orders.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Transient;

import com.bistroops.ordersdetails.model.OrdersDetailsVO;
//import com.bistroops.seat.model.SeatVO;
import com.bistroops.seat.model.SeatVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="orders")
public class OrdersVO {
	
	/*
	訂單, orders PRIMARY KEY(orders_no)

	orders_no     INT UNSIGNED '訂單編號'    AUTO_INCREMENT
	seat_no            CHAR(3)   '桌號',NOT NULL (FK)
	member_no INT      UNSIGNED  '會員編號',(FK)
	orders_time        DATETIME  '下單時間',
	orders_total       INT       '總金額',
	orders_discount_total  INT '折扣金額',
	orders_actual_price    INT '實付金額',
	orders_pay          VARCHAR(2)'付款方式', 1.現金 | 2.刷卡
	orders_details_no   INT UNSIGNED '訂單明細編號',NOT NULL (FK)
	 */
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="orders_no")
	private Integer orders;
	
	//桌號
	@ManyToOne
	@JoinColumn(name="seat_no",nullable=false)
	private SeatVO seatNo;
	
	@Column(name="member_no")
	private Integer memberNo;
	
	@Column(name="orders_time")
	private LocalDateTime ordersTime;
	
	@Column(name="orders_total") //總金額
	private Integer ordersTotal;
	
	@Column(name="orders_discount_total") //折扣金額
	private Integer ordersDiscountTotal;
	
	@Column(name="orders_actual_price") //實付金額
	private Integer ordersActualPrice; 
	
	@Column(name="orders_pay")
	private String ordersPay;
	
	//訂單明細
	@OneToMany(mappedBy = "orders", fetch = FetchType.EAGER)
	private List<OrdersDetailsVO> ordersDetails;
	
//	//回饋
//	@OneToMany(mappedBy="orders")
//	private List<FeedBackVO> feedBack;
	
	
	
	public OrdersVO() {
		super();
	}
	
	public void setOrders(Integer orders) {
		this.orders = orders;
	}
	
	public Integer getOrders() {
		return orders;
	}

	public void setOrdersNo(Integer orders) {
		this.orders = orders;
	}

	public SeatVO getSeatNo() {
		return seatNo;
	}

	public void setSeatNo(SeatVO seatNo) {
		this.seatNo = seatNo;
	}


	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public LocalDateTime getOrdersTime() {
		return ordersTime;
	}

	public void setOrdersTime(LocalDateTime ordersTime) {
		this.ordersTime = ordersTime;
	}

	public Integer getOrdersTotal() {
		return ordersTotal;
	}

	public void setOrdersTotal(Integer ordersTotal) {
		this.ordersTotal = ordersTotal;
	}

	public Integer getOrdersDiscountTotal() {
		return ordersDiscountTotal;
	}

	public void setOrdersDiscountTotal(Integer ordersDiscountTotal) {
		this.ordersDiscountTotal = ordersDiscountTotal;
	}

	public Integer getOrdersActualPrice() {
		return ordersActualPrice;
	}

	public void setOrdersActualPrice(Integer ordersActualPrice) {
		this.ordersActualPrice = ordersActualPrice;
	}

	public String getOrdersPay() {
		return ordersPay;
	}

	public void setOrdersPay(String ordersPay) {
		this.ordersPay = ordersPay;
	}

	public List<OrdersDetailsVO> getOrdersDetails() {
		return ordersDetails;
	}

	public void setOrdersDetails(List<OrdersDetailsVO> orders) {
		this.ordersDetails = orders;
	}
	
	@Transient 
	public String getStatus() {
		boolean hasItem=false ,allCancelled=true,allServed =true;
		for(OrdersDetailsVO item :ordersDetails) {  
			hasItem =true; 
			if (!"已取消".equals(item.getOdStatus())) { //不是已取消
				allCancelled=false;
				if(!"已送達".equals(item.getOdStatus())) allServed=false;//不是已送達
			}
		}
		if(hasItem &&allCancelled) return "CANCELLED";
		if(!hasItem || !allServed)return "ACTIVE"; //有明細或沒有全部已送達
		return (ordersPay != null && !ordersPay.isEmpty()) ?"PAID"  :"DONE"; //已結帳或是完畢
		
		
		//CANCELLED 取消/ACTIVE 用餐中 /PAID 已付款 /DONE 餐點送完未付款 
	}



}
