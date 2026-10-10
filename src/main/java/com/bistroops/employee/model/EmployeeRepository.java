package com.bistroops.employee.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository
        extends JpaRepository<EmployeeVO, Integer> {

}