package com.team.chatapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Defaults to "*" so nothing breaks if the property is absent.
     * Dev 6 can override in application.properties:
     *   app.allowed-origins=https://our-app.up.railway.app
     */
    @Value("${app.allowed-origins:*}")
    private String[] allowedOrigins;

    /** Contract paths - do not change without telling Dev 3 and Dev 4. */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                // {server sends every 10s, server expects client every 10s}
                // Keeps the connection alive through Railway's proxy timeout.
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(heartbeatScheduler());

        registry.setApplicationDestinationPrefixes("/app");
    }

    /** enableSimpleBroker sends no heartbeats without a scheduler. */
    @Bean
    public TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    /** Stops one oversized message from exhausting server memory. */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024);       // 64 KB per message
        registration.setSendBufferSizeLimit(512 * 1024);   // 512 KB queued per session
        registration.setSendTimeLimit(20 * 1000);          // 20s to flush before disconnect
    }
}
