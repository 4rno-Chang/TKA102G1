package com.bistroops.config;

import com.bistroops.interceptor.MemberLoginInterceptor;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bistroops.interceptor.EmployeeLoginInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final EmployeeLoginInterceptor employeeLoginInterceptor;
	private final MemberLoginInterceptor memberLoginInterceptor;
	

	public WebMvcConfig(
	        EmployeeLoginInterceptor employeeLoginInterceptor,
	        MemberLoginInterceptor memberLoginInterceptor) {

	    this.employeeLoginInterceptor = employeeLoginInterceptor;
	    this.memberLoginInterceptor = memberLoginInterceptor;
	}

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(employeeLoginInterceptor)
        .addPathPatterns(
                "/staff",
                "/staff/**",
                "/api/employees",
                "/api/employees/**"
        )
        .excludePathPatterns(
                "/staff/login",
                "/api/employees/login",
                "/staff/checkout/ecpay-return",   // 綠界伺服器付款通知（綠界沒有登入）
                "/staff/checkout/ecpay-result"    // 付款後小視窗被綠界帶回（跨網站，瀏覽器不會帶登入 cookie）
        );
        
     // 會員登入期限檢查
        registry.addInterceptor(memberLoginInterceptor)
                .addPathPatterns(
                        "/bistroops",
                        "/bistroops/**",
                        "/member/**"
                );
        
        
    }
}
