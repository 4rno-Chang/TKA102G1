package com.bistroops.interceptor;

import com.bistroops.employee.model.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice(annotations = Controller.class)
public class StaffPermissionAdvice {

    private final EmployeeService employeeService;

    public StaffPermissionAdvice(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @ModelAttribute
    public void addPermission(Model model, HttpServletRequest request) {

        // 只處理員工後台頁面
        String path = request.getRequestURI();

        if (!path.startsWith("/staff")
                || path.equals("/staff/login")) {
            return;
        }

        HttpSession session = request.getSession(false);

        Integer empNo = session == null ? null
                : (Integer) session.getAttribute("employeeNo");

        // 工作面板權限（1：內場、2：外場）
        boolean hasWorkboard = employeeService.hasPermission(empNo, 1) || employeeService.hasPermission(empNo, 2);
        model.addAttribute("hasWorkboard", hasWorkboard);
        
        // 入座管理權限（3）
        boolean hasSeating = employeeService.hasPermission(empNo, 3);
        model.addAttribute("hasSeating", hasSeating);
        
        // 訂候位管理權限（4）
        boolean hasBooking = employeeService.hasPermission(empNo, 4);

        model.addAttribute("hasBooking", hasBooking);
        
        // 訂單管理：權限 (5)
        boolean hasOrders = employeeService.hasPermission(empNo, 5);
        model.addAttribute("hasOrders", hasOrders);
        
        // 結帳管理：權限（6）
        boolean hasCheckout = employeeService.hasPermission(empNo, 6);
        model.addAttribute("hasCheckout", hasCheckout);

        // 後台管理權限（7）
        boolean canManageBackend = employeeService.hasPermission(empNo, 7);

        model.addAttribute("canManageBackend", canManageBackend);
    }
}