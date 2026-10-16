package com.bistroops.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.bistroops.orderscart.controller.CartWebSocketHandler;

@Configuration
@EnableWebSocket
public class CartWebSocketConfig implements WebSocketConfigurer {

    private final CartWebSocketHandler cartWebSocketHandler;

    public CartWebSocketConfig(CartWebSocketHandler cartWebSocketHandler) {
        this.cartWebSocketHandler = cartWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(cartWebSocketHandler, "/bistroops/order/ws")
                .addInterceptors(new HttpSessionHandshakeInterceptor(List.of("orderSeatingKey")));
    }
}
