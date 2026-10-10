package com.bistroops.employee.model;

import com.bistroops.permission.model.PermissionVO;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "emp_permission")
public class EmployeePermissionVO {
	
	@EmbeddedId
	private EmployeePermissionId ids = new EmployeePermissionId();
	
	@MapsId("empNo")
	@ManyToOne
	@JoinColumn(name = "emp_no",referencedColumnName = "emp_no")
	private EmployeeVO employee;
	
	@JsonIgnore
	@MapsId("permNo")
	@ManyToOne
	@JoinColumn(name = "perm_no", referencedColumnName = "perm_no")
	private PermissionVO permission;

	public EmployeePermissionVO() {
	}
	
	public EmployeePermissionId getIds() {
	    return ids;
	}

	public void setIds(EmployeePermissionId ids) {
	    this.ids = ids;
	}

	public EmployeeVO getEmployee() {
		return employee;
	}

	public void setEmployee(EmployeeVO employee) {
		this.employee = employee;
	}

	public PermissionVO getPermission() {
		return permission;
	}

	public void setPermission(PermissionVO permission) {
		this.permission = permission;
	}

	@Override
	public String toString() {
	    return "EmployeePermissionVO [ids=" + ids + "]";
	}

	

}

