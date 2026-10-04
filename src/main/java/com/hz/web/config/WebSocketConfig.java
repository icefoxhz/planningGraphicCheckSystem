package com.hz.web.config;

import com.hz.web.service.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


/**
 * @author saber
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Value("${myProject.webSocket.paths}")
    private String paths;

    @Autowired
    private WebsocketInterceptor customWebsocketInterceptor;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                // 设置处理器处理/custom/**
                .addHandler(webSocketHandler, paths)
                // 允许跨越
                .setAllowedOrigins("*")
                // 设置监听器
                .addInterceptors(customWebsocketInterceptor);
    }


}
