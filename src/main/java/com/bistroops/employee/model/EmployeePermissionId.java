package com.bistroops.employee.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EmployeePermissionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "emp_no")
    private Integer empNo;

    @Column(name = "perm_no")
    private Integer permNo;

    public EmployeePermissionId() {
    }

    public EmployeePermissionId(Integer empNo, Integer permNo) {
        this.empNo = empNo;
        this.permNo = permNo;
    }

    public Integer getEmpNo() {
        return empNo;
    }

    public void setEmpNo(Integer empNo) {
        this.empNo = empNo;
    }

    public Integer getPermNo() {
        return permNo;
    }

    public void setPermNo(Integer permNo) {
        this.permNo = permNo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        EmployeePermissionId other = (EmployeePermissionId) obj;

        return Objects.equals(empNo, other.empNo)
                && Objects.equals(permNo, other.permNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(empNo, permNo);
    }
}