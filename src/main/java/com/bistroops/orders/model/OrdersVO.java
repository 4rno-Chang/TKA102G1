package com.bistroops.orders.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.bistroops.feedback.model.FeedBackVO;
import com.bistroops.ordersdetails.model.OrdersDetailsVO;
//import com.bistroops.seat.model.SeatVO;

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
	private Integer ordersNo;
	
	//桌號
//	@ManyToOne
//	@JoinColumn(name="seat_no",nullable=false)
//	private SeatVO seatNo;
	
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
	private Set<OrdersDetailsVO> ordersDetails;
	
	//回饋
//	@OneToMany(mappedBy="orders")
//	private Set<FeedBackVO> feedBack = new HashSet<>();
	
	
	
	public OrdersVO() {
		super();
	}

	public Integer getOrdersNo() {
		return ordersNo;
	}

	public void setOrdersNo(Integer ordersNo) {
		this.ordersNo = ordersNo;
	}

//	public SeatVO getSeatNo() {
//		return seatNo;
//	}
//
//	public void setSeatNo(SeatVO seatNo) {
//		this.seatNo = seatNo;
//	}


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

//	public Set<OrdersDetailsVO> getOrdersDetails() {
//		return ordersDetails;
//	}
//
//	public void setOrdersDetails(Set<OrdersDetailsVO> orders) {
//		this.ordersDetails = orders;
//	}


}
