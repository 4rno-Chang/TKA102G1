package com.bistroops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.bistroops.employee.model.EmployeeService;
import com.bistroops.employee.model.EmployeeVO;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice(annotations = Controller.class)
public class StaffAccountModelAdvice {

    @Autowired
    private EmployeeService employeeService;

    @ModelAttribute("staffEmployeeName")
    public String staffEmployeeName(HttpSession session) {

        // 1. 取得目前登入的員工編號
        Integer empNo = (Integer) session.getAttribute("employeeNo");

        // 2. 沒有登入就不顯示姓名
        if (empNo == null) {
            return null;
        }

        // 3. 從資料庫查詢員工
        EmployeeVO employee = employeeService.getEmployeeById(empNo)
                .orElse(null);

        // 4. 員工不存在或已停用
        if (employee == null || !"1".equals(employee.getEmpStatus())) {
            return null;
        }

        // 5. 回傳登入員工姓名
        return employee.getEmpName();
    }
}