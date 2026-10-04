package com.hz.web.config;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author saber
 */
@Configuration
public class AsyncHttpClientConfig {

    @Bean(destroyMethod = "close") // Spring 关闭时自动关闭资源
    public AsyncHttpClient asyncHttpClient() {
        DefaultAsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(5000)
                .setRequestTimeout(10000)
                .setMaxConnections(10000)
                .setMaxConnectionsPerHost(200)
                .build();
        return new DefaultAsyncHttpClient(config);
    }
}
