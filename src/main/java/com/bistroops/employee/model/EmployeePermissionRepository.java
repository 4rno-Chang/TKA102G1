package com.bistroops.employee.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePermissionRepository
        extends JpaRepository<EmployeePermissionVO, EmployeePermissionId> {

    // 查詢指定員工的權限
    List<EmployeePermissionVO> findByEmployee_EmpNo(Integer empNo);

    // 刪除指定員工的權限
    void deleteByEmployee_EmpNo(Integer empNo);
}