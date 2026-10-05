package com.bistroops.member.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MEMBER")
public class MemberVO {
	
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MEM_NO")
	private Integer memNo;
	
	@Column(name = "MEM_TEL")
	private String memTel;
	
	@Column(name = "MEM_NAME")
	private String memName;
	
	@Column(name = "MEM_PASSWORD")
	private String memPassword;
	
	@Column(name = "MEM_MAIL")
	private String memMail;
	
	@Column(name = "MEM_BARCODE")
	private String memBarcode;
	
	@Column(name = "MEM_TAG")
	private String  memTag;
	
	@Column(name = "MEM_BIRTH")
	private LocalDate memBirth;
	
	@Column(name = "MEM_FB")
	private String memFb;
	
	@Column(name = "MEM_LINE")
	private String memLine;
	
	@Column(name = "MEM_GOO")
	private String memGoo;
	
	@Column(name = "MEM_X")
	private String memX;
	
	@Column(name = "MEM_STATUS")
	private String memStatus;
	
	public MemberVO() {
	    super();
	}

	//password不放入toString，避免敏感資料被輸出
	@Override
	public String toString() {
		return "MemberVO [memNo=" + memNo + ", memTel=" + memTel + ", memName=" + memName
				+ ", memMail=" + memMail + ", memBarcode=" + memBarcode + ", memTag="
				+ memTag + ", memBirth=" + memBirth + ", memFb=" + memFb + ", memLine=" + memLine + ", memGoo=" + memGoo
				+ ", memX=" + memX + ", memStatus=" + memStatus + "]";
	}

	public Integer getMemNo() {
		return memNo;
	}

	public void setMemNo(Integer memNo) {
		this.memNo = memNo;
	}

	public String getMemTel() {
		return memTel;
	}

	public void setMemTel(String memTel) {
		this.memTel = memTel;
	}

	public String getMemName() {
		return memName;
	}

	public void setMemName(String memName) {
		this.memName = memName;
	}

	public String getMemPassword() {
		return memPassword;
	}

	public void setMemPassword(String memPassword) {
		this.memPassword = memPassword;
	}

	public String getMemMail() {
		return memMail;
	}

	public void setMemMail(String memMail) {
		this.memMail = memMail;
	}

	public String getMemBarcode() {
		return memBarcode;
	}

	public void setMemBarcode(String memBarcode) {
		this.memBarcode = memBarcode;
	}

	public String getMemTag() {
		return memTag;
	}

	public void setMemTag(String memTag) {
		this.memTag = memTag;
	}

	public LocalDate getMemBirth() {
		return memBirth;
	}

	public void setMemBirth(LocalDate memBirth) {
		this.memBirth = memBirth;
	}

	public String getMemFb() {
		return memFb;
	}

	public void setMemFb(String memFb) {
		this.memFb = memFb;
	}

	public String getMemLine() {
		return memLine;
	}

	public void setMemLine(String memLine) {
		this.memLine = memLine;
	}

	public String getMemGoo() {
		return memGoo;
	}

	public void setMemGoo(String memGoo) {
		this.memGoo = memGoo;
	}

	public String getMemX() {
		return memX;
	}

	public void setMemX(String memX) {
		this.memX = memX;
	}

	public String getMemStatus() {
		return memStatus;
	}

	public void setMemStatus(String memStatus) {
		this.memStatus = memStatus;
	}

}
