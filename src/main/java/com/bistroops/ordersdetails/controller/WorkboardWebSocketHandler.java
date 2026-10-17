package com.bistroops.ordersdetails.controller;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WorkboardWebSocketHandler extends TextWebSocketHandler{

	
	// 所有連上來的工作面板（不用分組：大家看的是同一份訂單）
	private final Set<WebSocketSession> sessions =new CopyOnWriteArraySet<>();
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		// 包一層「可以同時被送訊息」的外殼（兩個員工同時按出餐時，不會互相卡住）
		sessions.add(new ConcurrentWebSocketSessionDecorator(session,5000,64*1024));
	} 
	@Override
	public void afterConnectionClosed(WebSocketSession session,CloseStatus status) {
		sessions.removeIf(s->s.getId().equals(session.getId()));// 用連線編號找到它、拿掉
	}
	
	//通知工作面板
	public void notifyChanged() {
		TextMessage message =new TextMessage("workboard-changed");
		for(WebSocketSession s : sessions) {
			try {
				if(s.isOpen()) {
					s.sendMessage(message);
				}
			}catch(Exception e){
				// 送不出去（例如剛好斷線）就跳過，不影響其他平板，也不影響原本的出餐、送達
				System.out.println("工作面板通知失敗："+e.getMessage());
			}
		}
	}
	

	
	
	
	
	
}
