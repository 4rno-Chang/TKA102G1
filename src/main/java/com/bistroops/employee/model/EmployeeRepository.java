package com.bistroops.employee.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<EmployeeVO, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmployeeVO e where e.empNo = :empNo")
    Optional<EmployeeVO> findForReactivation(@Param("empNo") Integer empNo);
}
