
package com.bistroops.interceptor;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class EmployeeLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        HttpSession session = request.getSession(false);

        Integer empNo = session == null ? null
                : (Integer) session.getAttribute("employeeNo");

        Long expireAt = session == null ? null
                : (Long) session.getAttribute("employeeExpireAt");

        // 尚未登入，或登入已超過 12 小時
        if (empNo == null || expireAt == null
                || System.currentTimeMillis() >= expireAt) {

            if (session != null) {
                session.removeAttribute("employeeNo");
                session.removeAttribute("employeeExpireAt");
            }

            // API 回傳 401，不導向 HTML 登入頁
            if (request.getRequestURI().startsWith("/api/")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.sendRedirect(request.getContextPath() + "/staff/login");
            }

            return false;
        }

        return true;
    }
}
