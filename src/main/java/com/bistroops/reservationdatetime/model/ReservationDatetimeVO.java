package com.bistroops.reservationdatetime.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation_datetime")
public class ReservationDatetimeVO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "rsv_dt_no")
	private Long rsvDtNo;

	@Column(name = "rsv_dt_datetime")
	private LocalDateTime rsvDtDatetime;

	public ReservationDatetimeVO() {
	}

	public Long getRsvDtNo() {
		return rsvDtNo;
	}

	public void setRsvDtNo(Long rsvDtNo) {
		this.rsvDtNo = rsvDtNo;
	}

	public LocalDateTime getRsvDtDatetime() {
		return rsvDtDatetime;
	}

	public void setRsvDtDatetime(LocalDateTime rsvDtDatetime) {
		this.rsvDtDatetime = rsvDtDatetime;
	}
}
