package com.bistroops.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.bistroops.ordersdetails.controller.WorkboardWebSocketHandler;
@Configuration
public class WorkboardWebSocketConfig implements WebSocketConfigurer{
	
	
	private final WorkboardWebSocketHandler workboardWebSocketHandler;
	
	public WorkboardWebSocketConfig(WorkboardWebSocketHandler workboardWebSocketHandler) {
		this.workboardWebSocketHandler= workboardWebSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// TODO Auto-generated method stub
		registry.addHandler(workboardWebSocketHandler, "/staff/workboard/ws")
		  // 允許本機和 ngrok 連線（Spring 預設只接受同一個網站，經過 ngrok 時會被當成別的網站而拒絕）
		.setAllowedOriginPatterns("http://localhost:*", "https://*.ngrok-free.dev");
	}
}
