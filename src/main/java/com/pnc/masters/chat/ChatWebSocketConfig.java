package com.pnc.masters.chat;

import com.pnc.masters.security.AppOriginProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatStompAuthChannelInterceptor authInterceptor;
    private final String[] allowedOrigins;
    private final TaskScheduler heartbeatScheduler = heartbeatScheduler();

    public ChatWebSocketConfig(ChatStompAuthChannelInterceptor authInterceptor, AppOriginProperties origins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = origins.originList().toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {25_000, 25_000})
                .setTaskScheduler(heartbeatScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat").setAllowedOrigins(allowedOrigins);
        registry.addEndpoint("/ws/chat").setAllowedOrigins(allowedOrigins).withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Bean
    TaskScheduler chatHeartbeatScheduler() {
        return heartbeatScheduler;
    }

    private static TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("chat-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
