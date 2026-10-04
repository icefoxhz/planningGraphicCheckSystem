package com.hz.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * @author saber
 * 用来处理webscocket拦截器
 */
@Component
public class WebsocketInterceptor extends HttpSessionHandshakeInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketInterceptor.class);

    /**
     * 建立连接时
     *
     * @param request    the current request
     * @param response   the current response
     * @param wsHandler  the target WebSocket handler
     * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
     *                   session; the provided attributes are copied, the original map is not used.
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
//        System.out.println("websocket interceptor beforeHandshake");
//        ServletServerHttpRequest req = (ServletServerHttpRequest) request;
//        ServletServerHttpResponse res = (ServletServerHttpResponse) response;
//        String token = req.getServletRequest().getParameter("token");
//        String username = req.getServletRequest().getParameter("username");
//        logger.info("建立连接....token:{} username:{}", token, username);
//        logger.info("attributes:{}", attributes);
//        attributes.put("token", token);
//        attributes.put("username", username);
        return true;
    }

    /**
     * 成功建立连接后
     *
     * @param request   the current request
     * @param response  the current response
     * @param wsHandler the target WebSocket handler
     * @param exception an exception raised during the handshake, or {@code null} if none
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
//        logger.info("连接成功....");
//        //其他业务代码
//        super.afterHandshake(request, response, wsHandler, exception);
    }
}
