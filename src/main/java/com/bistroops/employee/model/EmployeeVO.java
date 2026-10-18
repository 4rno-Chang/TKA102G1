package com.bistroops.employee.model;

import java.util.Set;

import jakarta.persistence.Lob;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "employee")
public class EmployeeVO {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "emp_no")
	private Integer empNo;
	
	@Column(name = "emp_name")
	private String empName;
	
	@JsonIgnore
	@Column(name = "emp_password")
	private String empPassword;
	
	// 是否已完成首次密碼修改
	@JsonIgnore
	@Column(name = "emp_pwd_changed", nullable = false)
	private Boolean empPwdChanged = false;
	
	@Column(name = "emp_tel")
	private String empTel;

	@Column(name = "emp_email", length = 254)
	private String empEmail;

	@JsonIgnore
	@Column(name = "emp_temp_password_expires_at")
	private java.time.Instant empTempPasswordExpiresAt;

	public String getEmpEmail() { return empEmail; }
	public void setEmpEmail(String empEmail) { this.empEmail = empEmail; }
	public java.time.Instant getEmpTempPasswordExpiresAt() { return empTempPasswordExpiresAt; }
	public void setEmpTempPasswordExpiresAt(java.time.Instant expiresAt) {
	    this.empTempPasswordExpiresAt = expiresAt;
	}
	
	@Column(name = "emp_ice")
	private String empIce;
	
	@Column(name = "emp_icetel")
	private String empIcetel;
	
	@Column(name = "emp_add")
	private String empAdd;
	
	@Column(name = "emp_sal")
	private Integer empSal;
	
	@Column(name = "emp_status")
	private String empStatus;
	

	@JsonIgnore
	@Lob
	@Column(name = "emp_photo", columnDefinition = "MEDIUMBLOB")
	private byte[] empPhoto;

	
	@JsonIgnore
	@OneToMany(mappedBy = "employee", cascade = CascadeType.ALL,orphanRemoval = true)
	private Set<EmployeePermissionVO> employeepermissions = new HashSet<>();
	

	public EmployeeVO() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Integer getEmpNo() {
		return empNo;
	}


	public void setEmpNo(Integer empNo) {
		this.empNo = empNo;
	}


	public String getEmpName() {
		return empName;
	}


	public void setEmpName(String empName) {
		this.empName = empName;
	}


	public String getEmpPassword() {
		return empPassword;
	}


	public void setEmpPassword(String empPassword) {
		this.empPassword = empPassword;
	}
	
	public Boolean getEmpPwdChanged() {
	    return empPwdChanged;
	}

	public void setEmpPwdChanged(Boolean empPwdChanged) {
	    this.empPwdChanged = empPwdChanged;
	}


	public String getEmpTel() {
		return empTel;
	}


	public void setEmpTel(String empTel) {
		this.empTel = empTel;
	}


	public String getEmpIce() {
		return empIce;
	}


	public void setEmpIce(String empIce) {
		this.empIce = empIce;
	}


	public String getEmpIcetel() {
		return empIcetel;
	}


	public void setEmpIcetel(String empIcetel) {
		this.empIcetel = empIcetel;
	}


	public String getEmpAdd() {
		return empAdd;
	}


	public void setEmpAdd(String empAdd) {
		this.empAdd = empAdd;
	}


	public Integer getEmpSal() {
		return empSal;
	}


	public void setEmpSal(Integer empSal) {
		this.empSal = empSal;
	}


	public String getEmpStatus() {
		return empStatus;
	}


	public void setEmpStatus(String empStatus) {
		this.empStatus = empStatus;
	}


	public Set<EmployeePermissionVO> getEmployeepermissions() {
		return employeepermissions;
	}


	public void setEmployeepermissions(Set<EmployeePermissionVO> employeepermissions) {
		this.employeepermissions = employeepermissions;
	}


	public byte[] getEmpPhoto() {
	    return empPhoto;
	}
	
	public void setEmpPhoto(byte[] empPhoto) {
	    this.empPhoto = empPhoto;
	}


	@Override
	public String toString() {
	    return "EmployeeVO [empNo=" + empNo + ", empName=" + empName + ", empTel=" + empTel + ", empStatus=" + empStatus + "]";
	}

	
	


}
