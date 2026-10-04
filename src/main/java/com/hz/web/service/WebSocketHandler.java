package com.hz.web.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * @author saber
 * webscoket 处理器
 */
@Component
public class WebSocketHandler extends TextWebSocketHandler {
    @Autowired
    WebSocketService webSocketService;

    /**
     * 收到客户端消息时触发的回调
     *
     * @param session 连接对象
     * @param message 消息体
     */
    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws IOException, InterruptedException {
        webSocketService.handleMessage(session, message.getPayload());

    }

    /**
     * 建立连接后触发的回调
     *
     * @param session 连接对象
     */
    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        webSocketService.handleConnect(session);
    }

    /**
     * 断开连接后触发的回调
     *
     * @param session 连接对象
     * @param status  状态
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        webSocketService.handleClose(session);
    }

    /**
     * 传输消息出错时触发的回调
     *
     * @param session   连接对象
     * @param exception 异常
     * @throws Exception 异常
     */
    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) throws Exception {
        webSocketService.handleError(session, exception);
    }
}
