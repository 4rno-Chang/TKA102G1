package com.bistroops.ordersdetails.model;

import java.time.LocalDateTime;

import com.bistroops.meal.model.MealVO;
import com.bistroops.orders.model.OrdersVO;
import com.bistroops.promote.model.PromoteVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="orders_details")
public class OrdersDetailsVO {

	/*
	訂單明細, orders_details   PRIMARY KEY(orders_details_no)

	orders_details_no	 INT UNSIGNED	'訂單明細編號',NOT NULL
	orders_no       INT UNSIGNED  '訂單編號', NOT NULL
	meal_no         INT UNSIGNED  '菜品編號', NOT NULL
	promote_no      INT UNSIGNED  '活動編號',
	od_meal_num     INT  '菜品數量',
	od_discount_price INT  '下單時菜品單價',
	od_discount_total INT  '折扣金額',
	od_actual_price   INT  '小計',
	od_comment   VARCHAR(30)  '備註',
	od_status    VARCHAR(5)  '訂單狀態', 內場：出餐、等待送餐、已完餐 |外場：製作中、已送達、已完餐
	od_served_at DATETIME  '送達時間'  NULL

	*/

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="orders_details_no")
	private Integer ordersDetailsNo;

	@ManyToOne
	@JoinColumn(name="orders_no",referencedColumnName="orders_no", nullable=false)
	private OrdersVO orders;

	@ManyToOne
	@JoinColumn(name="meal_no" ,referencedColumnName="meal_no", nullable=false)
	private MealVO mealNo;

	@ManyToOne
	@JoinColumn(name="promote_no" ,referencedColumnName="promote_no")
	private PromoteVO promoteNo;

	@Column(name = "od_meal_num")
	private Integer odMealNum;

	@Column(name = "od_discount_price")
	private Integer odDiscountPrice;

	@Column(name = "od_discount_total")
	private Integer odDiscountTotal;

	@Column(name = "od_actual_price")
	private Integer odActualPrice;

	@Column(name = "od_comment")
	private String odComment;

	@Column(name = "od_status")
	private String odStatus;
	
	@Column(name = "od_served_at")
	private LocalDateTime odServedAt;
	

	public OrdersDetailsVO() {
		super();
	}

	public Integer getOrdersDetailsNo() {
		return ordersDetailsNo;
	}

	public void setOrdersDetailsNo(Integer ordersDetailsNo) {
		this.ordersDetailsNo = ordersDetailsNo;
	}

	public OrdersVO getOrders() {
		return orders;
	}

	public void setOrders(OrdersVO orders) {
		this.orders = orders;
	}

	public MealVO getMealNo() {
		return mealNo;
	}

	public void setMealNo(MealVO mealNo) {
		this.mealNo = mealNo;
	}

	public PromoteVO getPromoteNo() {
		return promoteNo;
	}

	public void setPromoteNo(PromoteVO promoteNo) {
		this.promoteNo = promoteNo;
	}

	public Integer getOdMealNum() {
		return odMealNum;
	}

	public void setOdMealNum(Integer odMealNum) {
		this.odMealNum = odMealNum;
	}

	public Integer getOdDiscountPrice() {
		return odDiscountPrice;
	}

	public void setOdDiscountPrice(Integer odDiscountPrice) {
		this.odDiscountPrice = odDiscountPrice;
	}

	public Integer getOdDiscountTotal() {
		return odDiscountTotal;
	}

	public void setOdDiscountTotal(Integer odDiscountTotal) {
		this.odDiscountTotal = odDiscountTotal;
	}

	public Integer getOdActualPrice() {
		return odActualPrice;
	}

	public void setOdActualPrice(Integer odActualPrice) {
		this.odActualPrice = odActualPrice;
	}

	public String getOdComment() {
		return odComment;
	}

	public void setOdComment(String odComment) {
		this.odComment = odComment;
	}

	public String getOdStatus() {
		return odStatus;
	}

	public void setOdStatus(String odStatus) {
		this.odStatus = odStatus;
	}

	public LocalDateTime getOdServedAt() {
		return odServedAt;
	}

	public void setOdServedAt(LocalDateTime odServedAt) {
		this.odServedAt = odServedAt;
	}

}
