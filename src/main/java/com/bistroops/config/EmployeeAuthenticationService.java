package com.bistroops.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.bistroops.employee.model.EmployeeService;
import com.bistroops.employee.model.EmployeeVO;

@Service
public class EmployeeAuthenticationService {

    private final EmployeeService employeeService;

    public EmployeeAuthenticationService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public Authentication authenticate(Integer empNo, String password) {

        // 1. 驗證員工編號與密碼
        EmployeeVO employee = employeeService.login(empNo, password);

        if (employee == null) {
            return null;
        }

        // 2. 建立員工權限清單
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 3. 所有登入成功的員工都有基本身分
        authorities.add(new SimpleGrantedAuthority("EMPLOYEE"));

        // 4. 從 MySQL 查詢該員工所有權限
        List<Integer> permissions =
                employeeService.getEmployeePermissions(employee.getEmpNo());

        // 5. 動態轉換成 Spring Security 權限
        for (Integer permNo : permissions) {
            authorities.add(
                    new SimpleGrantedAuthority("PERM_" + permNo)
            );
        }

        // 6. 建立並回傳 Spring Security Authentication
        return new UsernamePasswordAuthenticationToken(
                employee.getEmpNo().toString(),
                null,
                authorities
        );
    }
}