package com.bistroops.feedback.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="feedback")
public class FeedBackVO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="feedback_no")
	private Integer feedbackNo;
	
//	@ManyToOne
//	@JoinColumn(name="orders_no",referencedColumnName = "orders_no")
//	private OrdersVO orders;

	@Column(name="fee_customer")
	private String feeCustomer;
	
	@Column(name="fee_customer_tel")
	private String feeCustomerTel;
	
	@Column(name="fee_customer_time")
	private LocalDateTime feeCustomerTime;
	
	@Column(name="fee_time", updatable = false)
	private LocalDateTime feeTime;
	
	@Column(name="fee_content")
	private String feeContent;
	
	@Column(name="fee_rating", columnDefinition = "tinyint")
	private Integer feeRating;
	
	public FeedBackVO() {
	}

	public Integer getFeedbackNo() {
		return feedbackNo;
	}

	public void setFeedbackNo(Integer feedbackNo) {
		this.feedbackNo = feedbackNo;
	}

//	public OrdersVO getOrders() {
//		return orders;
//	}
//
//	public void setOrders(OrdersVO orders) {
//		this.orders = orders;
//	}

	public String getFeeCustomer() {
		return feeCustomer;
	}

	public void setFeeCustomer(String feeCustomer) {
		this.feeCustomer = feeCustomer;
	}

	public String getFeeCustomerTel() {
		return feeCustomerTel;
	}

	public void setFeeCustomerTel(String feeCustomerTel) {
		this.feeCustomerTel = feeCustomerTel;
	}

	public LocalDateTime getFeeCustomerTime() {
		return feeCustomerTime;
	}

	public void setFeeCustomerTime(LocalDateTime feeCustomerTime) {
		this.feeCustomerTime = feeCustomerTime;
	}

	public LocalDateTime getFeeTime() {
		return feeTime;
	}

	public void setFeeTime(LocalDateTime feeTime) {
		this.feeTime = feeTime;
	}

	public String getFeeContent() {
		return feeContent;
	}

	public void setFeeContent(String feeContent) {
		this.feeContent = feeContent;
	}

	public Integer getFeeRating() {
		return feeRating;
	}

	public void setFeeRating(Integer feeRating) {
		this.feeRating = feeRating;
	}

	@Override
	public String toString() {
		return "feedbackVO [feedbackNo=" + feedbackNo + ", feeCustomer=" + feeCustomer + ", feeCustomerTel="
				+ feeCustomerTel + ", feeCustomerTime=" + feeCustomerTime + ", feeTime=" + feeTime + ", feeContent="
				+ feeContent + ", feeRating=" + feeRating + "]";
	}





}
