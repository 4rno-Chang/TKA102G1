
package com.bistroops.config;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import com.bistroops.employee.model.EmployeeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class EmployeePermissionAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final EmployeeService employeeService;

    public EmployeePermissionAuthorizationManager(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {

        HttpServletRequest request = context.getRequest();
        HttpSession session = request.getSession(false);

        // 1. 確認 Session 存在
        if (session == null) {
            return new AuthorizationDecision(false);
        }

        Integer empNo = (Integer) session.getAttribute("employeeNo");
        Long expireAt = (Long) session.getAttribute("employeeExpireAt");

        // 2. 確認員工登入資料與有效期限
        if (empNo == null || expireAt == null
                || System.currentTimeMillis() >= expireAt) {
            return new AuthorizationDecision(false);
        }

        // 3. 確認 Spring Security 的登入身分
        Authentication auth = authentication.get();

        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        boolean isEmployee = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("EMPLOYEE"::equals);

        if (!isEmployee) {
            return new AuthorizationDecision(false);
        }

        // 4. 確認 Security 身分與 Session 員工編號一致
        if (!empNo.toString().equals(auth.getName())) {
            return new AuthorizationDecision(false);
        }

        // 5. 取得目前請求的網址
        String path = request.getServletPath();

        // 6. 工作面板：權限 1、2
        if (path.equals("/staff/workboard")
                || path.startsWith("/staff/workboard/")) {

            // 內場操作：必須有權限 1
            if (path.equals("/staff/workboard/toServe")
                    || path.equals("/staff/workboard/backToCooking")
                    || path.equals("/staff/workboard/edit")
                    || path.equals("/staff/workboard/notes")
                    || path.equals("/staff/workboard/cancel")) {

                return new AuthorizationDecision(
                        employeeService.hasPermission(empNo, 1));
            }

            // 外場操作：必須有權限 2
            if (path.equals("/staff/workboard/served")) {

                return new AuthorizationDecision(
                        employeeService.hasPermission(empNo, 2));
            }

            // 工作面板畫面
            if (path.equals("/staff/workboard")) {

                String view = request.getParameter("view");

                boolean hasKitchen = employeeService.hasPermission(empNo, 1);
                boolean hasFloor = employeeService.hasPermission(empNo, 2);

                // 內場
                if ("kitchen".equals(view)) {
                    return new AuthorizationDecision(hasKitchen);
                }

                // 外場
                if ("floor".equals(view)) {
                    return new AuthorizationDecision(hasFloor);
                }

                // 已完成訂單：權限 1 或 2
                if ("done".equals(view)) {
                    return new AuthorizationDecision(hasKitchen || hasFloor);
                }

                // 未指定 view：只要有 1 或 2 就可以進入
                if (view == null || view.isBlank()) {
                    return new AuthorizationDecision(hasKitchen || hasFloor);
                }

                // 不認識的 view：拒絕
                return new AuthorizationDecision(false);
            }

            // 未列出的工作面板路徑一律拒絕
            return new AuthorizationDecision(false);
        }
        
        // 7. 入座管理：權限 3
        if (path.equals("/staff/seating")
                || path.startsWith("/staff/seating/")) {

            return new AuthorizationDecision(
                    employeeService.hasPermission(empNo, 3));
        }
        
        // 8. 訂候位管理：權限 4
        if (path.equals("/staff/reservations")
                || path.startsWith("/staff/reservations/")
                || path.equals("/bistroops/waiting/manage")
                || path.startsWith("/bistroops/waiting/manage/")) {

            return new AuthorizationDecision(
                    employeeService.hasPermission(empNo, 4));
        }
        
        // 9.訂單管理：權限 5
        if (path.equals("/staff/orders")
                || path.startsWith("/staff/orders/")) {
            return new AuthorizationDecision(
                    employeeService.hasPermission(empNo, 5));
        }
        
        // 10. 結帳管理：權限 6
        if (path.equals("/staff/checkout")
                || path.equals("/staff/checkout/pay")
                || path.equals("/staff/checkout/card")) {

            return new AuthorizationDecision(
                    employeeService.hasPermission(empNo, 6));
        }

        // 11. 其他原本受保護的後台管理功能：權限 7
        return new AuthorizationDecision(
                employeeService.hasPermission(empNo, 7));
    }
}
