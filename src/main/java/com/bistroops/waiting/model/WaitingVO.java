package com.bistroops.waiting.model;

import java.time.LocalTime;

import com.bistroops.member.model.MemberVO;
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
@Table(name = "waiting")
public class WaitingVO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "waiting_no")
	private Long waitingNo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_type_no", nullable = false)
	private SeatTypeVO seatType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mem_no")
	private MemberVO member;

	@Column(name = "waiting_tel", nullable = false, length = 10)
	private String waitingTel;

	@Column(name = "waiting_name", length = 30)
	private String waitingName;

	@Column(name = "waiting_comment", length = 20)
	private String waitingComment;

	@Column(name = "waiting_status", length = 3)
	private String waitingStatus;

	@Column(name = "waiting_notify_time")
	private LocalTime waitingNotifyTime;

	public WaitingVO() {
	}

	public Long getWaitingNo() {
		return waitingNo;
	}

	public void setWaitingNo(Long waitingNo) {
		this.waitingNo = waitingNo;
	}

	public SeatTypeVO getSeatType() {
		return seatType;
	}

	public void setSeatType(SeatTypeVO seatType) {
		this.seatType = seatType;
	}

	public MemberVO getMember() {
		return member;
	}

	public void setMember(MemberVO member) {
		this.member = member;
	}

	public String getWaitingTel() {
		return waitingTel;
	}

	public void setWaitingTel(String waitingTel) {
		this.waitingTel = waitingTel;
	}

	public String getWaitingName() {
		return waitingName;
	}

	public void setWaitingName(String waitingName) {
		this.waitingName = waitingName;
	}

	public String getWaitingComment() {
		return waitingComment;
	}

	public void setWaitingComment(String waitingComment) {
		this.waitingComment = waitingComment;
	}

	public String getWaitingStatus() {
		return waitingStatus;
	}

	public void setWaitingStatus(String waitingStatus) {
		this.waitingStatus = waitingStatus;
	}

	public LocalTime getWaitingNotifyTime() {
		return waitingNotifyTime;
	}

	public void setWaitingNotifyTime(LocalTime waitingNotifyTime) {
		this.waitingNotifyTime = waitingNotifyTime;
	}
}
