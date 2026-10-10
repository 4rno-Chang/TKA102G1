package com.bistroops.permission.model;

import java.util.Set;

import com.bistroops.employee.model.EmployeePermissionVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "permission")
public class PermissionVO {

    @Id
    @Column(name = "perm_no")
    private Integer permNo;

    @Column(name = "perm_exp")
    private String permExp;

    @JsonIgnore
    @OneToMany(mappedBy = "permission")
    private Set<EmployeePermissionVO> employeepermissions;

    public PermissionVO() {
    }

    public Integer getPermNo() {
        return permNo;
    }

    public void setPermNo(Integer permNo) {
        this.permNo = permNo;
    }

    public String getPermExp() {
        return permExp;
    }

    public void setPermExp(String permExp) {
        this.permExp = permExp;
    }

    public Set<EmployeePermissionVO> getEmployeepermissions() {
        return employeepermissions;
    }

    public void setEmployeepermissions(
            Set<EmployeePermissionVO> employeepermissions) {
        this.employeepermissions = employeepermissions;
    }

    @Override
    public String toString() {
        return "PermissionVO [permNo=" + permNo
                + ", permExp=" + permExp + "]";
    }
}