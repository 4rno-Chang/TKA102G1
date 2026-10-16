package com.bistroops.config;

import org.springframework.http.HttpStatus;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            EmployeePermissionAuthorizationManager permissionManager
    ) throws Exception {

        http
            // 過渡階段：暫時關閉 CSRF
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // 員工登入、登出及個人功能
                .requestMatchers(
                    "/api/employees/login",
                    "/api/employees/logout",
                    "/api/employees/me",
                    "/api/employees/change-password"
                ).permitAll()
                
                // 權限 1、2：工作面板及所有操作
                .requestMatchers(
                    "/staff/workboard",
                    "/staff/workboard/**"
                ).access(permissionManager)
                
                // 權限 3：入座管理及所有操作
                .requestMatchers(
                    "/staff/seating",
                    "/staff/seating/**"
                ).access(permissionManager)
                
             // 權限 4：新舊訂候位管理頁及所有操作。
                .requestMatchers(
                    "/staff/booking",
                    "/staff/booking/**",
                    "/staff/reservations",
                    "/staff/reservations/**",
                    "/bistroops/waiting/manage",
                    "/bistroops/waiting/manage/**"
                ).access(permissionManager)
                
                // 訂單管理：權限 5
                .requestMatchers(
                    "/staff/orders",
                    "/staff/orders/**"
                ).access(permissionManager)
                
                // 權限 6：結帳管理
                .requestMatchers(
                    "/staff/checkout",
                    "/staff/checkout/pay",
                    "/staff/checkout/card"
                ).access(permissionManager)

                // 權限 7：後台管理頁面
                .requestMatchers(
                    "/staff/employees",
                    "/staff/employees/**",
                    "/staff/promote",
                    "/staff/announcement-and-qa"
                ).access(permissionManager)

                // 權限 7：員工管理 API
                .requestMatchers(
                    "/api/employees",
                    "/api/employees/**"
                ).access(permissionManager)

                // 員工登入頁面允許公開
                .requestMatchers("/staff/login").permitAll()
                
                // 綠界付款回傳：不要求員工登入
                .requestMatchers(
                    "/staff/checkout/ecpay-return",
                    "/staff/checkout/ecpay-result"
                ).permitAll()

                // 其他後台頁面必須具有員工身分
                .requestMatchers(
                    "/staff",
                    "/staff/**"
                ).hasAuthority("EMPLOYEE")

                // 其他功能暫時維持原本機制
                .anyRequest().permitAll()
            )

         // 員工未登入或權限不足時的處理
            .exceptionHandling(ex -> ex

                // 401：未登入
                .authenticationEntryPoint((request, response, authException) -> {

                    String path = request.getServletPath();

                    if (path.startsWith("/api/")) {
                        response.sendError(HttpStatus.UNAUTHORIZED.value());
                    } else {
                        response.sendRedirect(
                            request.getContextPath() + "/staff/login"
                        );
                    }
                })

                // 403：已登入但沒有權限，或員工登入期限已過
                .accessDeniedHandler((request, response, accessDeniedException) -> {

                    String path = request.getServletPath();

                    if (path.startsWith("/api/")) {
                        response.sendError(HttpStatus.FORBIDDEN.value());
                    } else {
                        response.sendRedirect(
                            request.getContextPath() + "/staff/login"
                        );
                    }
                })
            )

            // 保留自訂登入與登出流程
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable());

        return http.build();
    }
}