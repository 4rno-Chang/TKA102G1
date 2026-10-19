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
        send(seatingKey, "cart-changed");
    }

    //通知這一組「有人正在送出訂單」，同桌其他手機的購物車和加菜的表單會先鎖住
    //送完（成功或失敗）會再送一次 cart-changed，收到就解開並更新購物車
    public void notifySubmitting(String seatingKey) {
        send(seatingKey, "cart-submitting");
    }

    //為了讓客人手機的「已點餐點」跟工作面板同步：通知這一組「店員改了已送出的訂單」
    //  客人手機收到 order-changed 會在背景拿最新的點餐頁，換掉「已點餐點」和購物車（order.js 的 A-7）
    public void notifyOrderChanged(String seatingKey) {
        send(seatingKey, "order-changed");
    }

    //把一段文字傳給這一組的每一支手機
    private void send(String seatingKey, String text) {
        Set<WebSocketSession> set = groups.get(seatingKey);
        if (set == null) {
            return;
        }
        TextMessage message = new TextMessage(text);
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
