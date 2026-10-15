package com.bistroops.interceptor;

import com.bistroops.employee.model.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class StaffPermissionInterceptor implements HandlerInterceptor {

    private final EmployeeService employeeService;

    public StaffPermissionInterceptor(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        HttpSession session = request.getSession(false);

        Integer empNo = session == null ? null
                : (Integer) session.getAttribute("employeeNo");

        if (employeeService.hasPermission(empNo, 7)) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}