package com.bistroops.promote.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import com.bistroops.promotemeal.model.PromoteMealVO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name="promote")
public class PromoteVO {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="promote_no")
	private Integer promoteNo;
	
	@Column(name="promote_name")
	private String promoteName;
	
	@Column(name="promote_begin")
	private LocalDateTime promoteBegin;
	
	@Column(name="promote_end")
	private LocalDateTime promoteEnd;
	
	@Column(name="promote_status")
	private String promoteStatus;
	
	@Column(name="promote_content", columnDefinition = "mediumtext")
	private String promoteContent;
	
	@Column(name="promote_img", columnDefinition = "mediumblob")
	private byte[] promoteImg;
	
	@OneToMany(mappedBy = "promote")
	@OrderBy("promote asc")
	private Set<PromoteMealVO> promoteMeals;
	
//	@OneToMany(mappedBy = "promote")
//	@OrderBy("ordersNo asc")
//	private Set<ordersDetails> ordersDetails;

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

	public String getPromoteStatus() {
		return promoteStatus;
	}

	public void setPromoteStatus(String promoteStatus) {
		this.promoteStatus = promoteStatus;
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

	public Set<PromoteMealVO> getPromoteMeals() {
		return promoteMeals;
	}

	public void setPromoteMeals(Set<PromoteMealVO> promoteMeals) {
		this.promoteMeals = promoteMeals;
	}

//	public Set<ordersDetails> getOrdersDetails() {
//		return ordersDetails;
//	}
//
//	public void setOrdersDetails(Set<ordersDetails> ordersDetails) {
//		this.ordersDetails = ordersDetails;
//	}

	@Override
	public String toString() {
		return "PromoteVO [promoteNo=" + promoteNo + ", promoteName=" + promoteName + ", promoteBegin=" + promoteBegin
				+ ", promoteEnd=" + promoteEnd + ", promoteStatus=" + promoteStatus + ", promoteContent="
				+ promoteContent + ", promoteImg=" + Arrays.toString(promoteImg) + ", promoteMeals=" + promoteMeals
				+ "]";
	}
	
	
	

}
