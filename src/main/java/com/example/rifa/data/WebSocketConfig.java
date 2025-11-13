package com.example.rifa.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 🔥 Habilita el broker en "/topic"
        registry.setApplicationDestinationPrefixes("/app"); // 🔥 Prefijo para los mensajes que envía el cliente
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // 🔥 Permitir cualquier origen usando allowedOriginPatterns()
                //.withSockJS(); // 🔥 Usa SockJS para mayor compatibilidad
    }
}

