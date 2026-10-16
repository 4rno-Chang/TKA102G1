package com.bistroops.orderscart.controller;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class CartWebSocketHandler extends TextWebSocketHandler {

    // 每一組客人的代碼
    private final Map<String, Set<WebSocketSession>> groups = new ConcurrentHashMap<>();

    //手機連上來的時候
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String key = (String) session.getAttributes().get("orderSeatingKey");
        if (key == null) {                                   // 沒有掃過 QR code：不讓它連
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        // 包一層「可以同時被送訊息」的外殼
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, 5000, 64 * 1024);
        groups.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(safe);
    }

    //手機離開的時候（關掉頁面、重新載入、網路斷掉）
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String key = (String) session.getAttributes().get("orderSeatingKey");
        if (key == null) {
            return;
        }
        Set<WebSocketSession> set = groups.get(key);
        if (set == null) {
            return;
        }
        set.removeIf(s -> s.getId().equals(session.getId())); // 用連線編號找到它、拿掉
        if (set.isEmpty()) {
            groups.remove(key);
        }
    }

    //通知這一組「購物車變了」
    public void notifyCartChanged(String seatingKey) {
        Set<WebSocketSession> set = groups.get(seatingKey);
        if (set == null) {
            return;
        }
        TextMessage message = new TextMessage("cart-changed");
        for (WebSocketSession s : set) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(message);
                }
            } catch (Exception e) {
                System.out.println(message);
            }
        }
    }
}
