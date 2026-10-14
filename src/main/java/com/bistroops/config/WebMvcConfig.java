
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
                "/api/employees/login"
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
