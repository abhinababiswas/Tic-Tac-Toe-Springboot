package com.tictactoe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Dedicated WebSocket & STOMP messaging configuration for real-time multiplayer.
 * Configures connection endpoints, user principal assignment, application prefixes,
 * and in-memory message broker.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        CustomHandshakeHandler handshakeHandler = new CustomHandshakeHandler();

        // Native WebSocket transport endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler);

        // SockJS fallback transport endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory simple broker for broadcast (/topic) and user-specific (/queue) messages
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for client-to-server messages routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-targeted messages (e.g. /user/queue/match)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Handshake handler that assigns a unique, non-null Principal to every anonymous WebSocket connection.
     * Enables Spring's SimpMessagingTemplate.convertAndSendToUser to target individual browser sessions.
     */
    public static class CustomHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String randomId = UUID.randomUUID().toString();
            return () -> randomId;
        }
    }
}
