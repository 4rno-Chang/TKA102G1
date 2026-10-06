package com.bistroops.reservation.model;

import java.time.LocalDateTime;

import com.bistroops.member.model.MemberVO;
import com.bistroops.reservationdatetime.model.ReservationDatetimeVO;
import com.bistroops.seattype.model.SeatTypeVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation")
public class ReservationVO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "rsv_no")
	private Long rsvNo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mem_no", nullable = false)
	private MemberVO member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rsv_dt_no", nullable = false)
	private ReservationDatetimeVO rsvDt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_type_no", nullable = false)
	private SeatTypeVO seatType;

	@Column(name = "rsv_create_time")
	private LocalDateTime rsvCreateTime;

	@Column(name = "rsv_status", length = 2)
	private String rsvStatus;

	@Column(name = "rsv_comment", length = 50)
	private String rsvComment;

	public ReservationVO() {
	}

	public Long getRsvNo() {
		return rsvNo;
	}

	public void setRsvNo(Long rsvNo) {
		this.rsvNo = rsvNo;
	}

	public MemberVO getMember() {
		return member;
	}

	public void setMember(MemberVO member) {
		this.member = member;
	}

	public ReservationDatetimeVO getRsvDt() {
		return rsvDt;
	}

	public void setRsvDt(ReservationDatetimeVO rsvDt) {
		this.rsvDt = rsvDt;
	}

	public SeatTypeVO getSeatType() {
		return seatType;
	}

	public void setSeatType(SeatTypeVO seatType) {
		this.seatType = seatType;
	}

	public LocalDateTime getRsvCreateTime() {
		return rsvCreateTime;
	}

	public void setRsvCreateTime(LocalDateTime rsvCreateTime) {
		this.rsvCreateTime = rsvCreateTime;
	}

	public String getRsvStatus() {
		return rsvStatus;
	}

	public void setRsvStatus(String rsvStatus) {
		this.rsvStatus = rsvStatus;
	}

	public String getRsvComment() {
		return rsvComment;
	}

	public void setRsvComment(String rsvComment) {
		this.rsvComment = rsvComment;
	}
}
