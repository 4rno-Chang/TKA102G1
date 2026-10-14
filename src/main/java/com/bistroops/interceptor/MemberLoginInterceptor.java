package com.bistroops.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class MemberLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return true;
        }

        Long expireAt = (Long) session.getAttribute("memberExpireAt");

     // 會員登入超過 2 小時，清除會員登入資訊
        if (expireAt != null && System.currentTimeMillis() >= expireAt) {
            session.removeAttribute("memberNo");
            session.removeAttribute("memberExpireAt");
        }
        return true;
    }
}