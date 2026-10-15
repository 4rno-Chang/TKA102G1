package com.bistroops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.bistroops.employee.model.EmployeeService;
import com.bistroops.employee.model.EmployeeVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice(annotations = Controller.class)
public class StaffAccountModelAdvice {

    @Autowired
    private EmployeeService employeeService;

    @ModelAttribute("staffEmployeeName")
    public String staffEmployeeName(HttpServletRequest request) {

        // 1. 取得現有的 session；沒有就不要新建
        //    （綠界付款完把小視窗帶回來時不會帶 cookie，這裡如果新建 session，會把員工的登入 cookie 蓋掉）
        HttpSession session = request.getSession(false);//只讀現有的 session，沒有就不要新建。
        if (session == null) {
            return null;
        }

        // 2. 取得目前登入的員工編號
        Integer empNo = (Integer) session.getAttribute("employeeNo");

        // 3. 沒有登入就不顯示姓名
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