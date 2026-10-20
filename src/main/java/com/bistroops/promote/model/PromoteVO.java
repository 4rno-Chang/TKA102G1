package com.bistroops.promote.model;

import java.time.LocalDateTime;
import java.util.Set;

import com.bistroops.ordersdetails.model.OrdersDetailsVO;
import com.bistroops.promotemeal.model.PromoteMealVO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name="promote")
public class PromoteVO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)

	@Column(name="promote_no")
	private Integer promoteNo;

	@Column(name="promote_name")
	@Size(max=10, message="活動名稱不得超過 10 個字(含符號)")
	@NotBlank(message = "請輸入活動名稱")
	private String promoteName;

	@Column(name="promote_begin")
	@NotNull(message = "請選擇開始日期")
	private LocalDateTime promoteBegin;

	@Column(name="promote_end")
	@NotNull(message = "請選擇結束日期")
	private LocalDateTime promoteEnd;

	@Column(name="promote_content", columnDefinition = "mediumtext")
	@Size(max=500, message="活動內容不得超過500字")
	private String promoteContent;
	
	@Lob
	@Column(name="promote_img", columnDefinition = "mediumblob")
	private byte[] promoteImg;

	// 整單折數（結帳時替整張訂單打折）
	@Column(name="promote_disc")
	@Min(value = 10, message = "整單折數請輸入 1～9 折（或兩位數的折數，例如 85）")
	@Max(value = 90, message = "整單折數請輸入 1～9 折（或兩位數的折數，例如 85）")
	private Integer promoteDisc;


	@Column(name="promote_rule")
	@Size(max=100, message="整單折扣的條件不得超過 100 字")
	private String promoteRule;

	@OneToMany(mappedBy = "promote",cascade = CascadeType.REMOVE)
	@OrderBy("promote asc")
	private Set<PromoteMealVO> promoteMeals;

	@OneToMany(mappedBy = "promote")
	@OrderBy("ordersDetailsNo asc")
	private Set<OrdersDetailsVO> ordersDetails;

	public PromoteVO() {
	}

	public Integer getPromoteNo() {
		return promoteNo;
	}

	public void setPromoteNo(Integer promoteNo) {
		this.promoteNo = promoteNo;
	}

	public String getPromoteName() {
		return promoteName;
	}

	public void setPromoteName(String promoteName) {
		this.promoteName = promoteName;
	}

	public LocalDateTime getPromoteBegin() {
		return promoteBegin;
	}

	public void setPromoteBegin(LocalDateTime promoteBegin) {
		this.promoteBegin = promoteBegin;
	}

	public LocalDateTime getPromoteEnd() {
		return promoteEnd;
	}

	public void setPromoteEnd(LocalDateTime promoteEnd) {
		this.promoteEnd = promoteEnd;
	}


	public String getPromoteContent() {
		return promoteContent;
	}

	public void setPromoteContent(String promoteContent) {
		this.promoteContent = promoteContent;
	}

	public byte[] getPromoteImg() {
		return promoteImg;
	}

	public void setPromoteImg(byte[] promoteImg) {
		this.promoteImg = promoteImg;
	}

	public Integer getPromoteDisc() {
		return promoteDisc;
	}

	public void setPromoteDisc(Integer promoteDisc) {
		this.promoteDisc = promoteDisc;
	}

	public String getPromoteRule() {
		return promoteRule;
	}

	public void setPromoteRule(String promoteRule) {
		this.promoteRule = promoteRule;
	}

	public Set<PromoteMealVO> getPromoteMeals() {
		return promoteMeals;
	}

	public void setPromoteMeals(Set<PromoteMealVO> promoteMeals) {
		this.promoteMeals = promoteMeals;
	}

	public Set<OrdersDetailsVO> getOrdersDetails() {
		return ordersDetails;
	}

	public void setOrdersDetails(Set<OrdersDetailsVO> ordersDetails) {
		this.ordersDetails = ordersDetails;
	}


}
