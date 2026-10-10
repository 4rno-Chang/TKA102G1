package com.bistroops.seat.model;
import java.time.LocalTime;

import com.bistroops.seattype.model.SeatTypeVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seat")
public class SeatVO implements java.io.Serializable{

	@ManyToOne
	@JoinColumn(name = "seat_type_no", referencedColumnName = "seat_type_no", insertable = false, updatable = false)
	private SeatTypeVO seatType;
	
/*
	@OneToMany(mappedBy = "seat_no", cascade = CascadeType.ALL)
	private Set<OrdersVO> orders;
*/

/*    seat_no CHAR(3) NOT NULL COMMENT '桌號',
  seat_type_no INT NOT NULL COMMENT '桌型編號',
  seat_status VARCHAR(5) COMMENT '桌位狀態',
  seat_time TIME COMMENT '入座時間',
*/
	@Id
	@Column(name = "seat_no")
	private String seatNo;
	
	@Column(name = "seat_type_no")
	private Integer seatTypeNo;
	
	@Column(name = "seat_status")
	private String seatStatus;
	
	@Column(name = "seat_time")
	private LocalTime seatTime;

	public SeatTypeVO getSeatType() {
		return seatType;
	}

	public void setSeatType(SeatTypeVO seatType) {
		this.seatType = seatType;
	}

	public String getSeatNo() {
		return seatNo;
	}

	public void setSeatNo(String seatNo) {
		this.seatNo = seatNo;
	}

	public Integer getSeatTypeNo() {
		return seatTypeNo;
	}

	public void setSeatTypeNo(Integer seatTypeNo) {
		this.seatTypeNo = seatTypeNo;
	}

	public String getSeatStatus() {
		return seatStatus;
	}

	public void setSeatStatus(String seatStatus) {
		this.seatStatus = seatStatus;
	}

	public LocalTime getSeatTime() {
		return seatTime;
	}

	public void setSeatTime(LocalTime seatTime) {
		this.seatTime = seatTime;
	}

	

}

