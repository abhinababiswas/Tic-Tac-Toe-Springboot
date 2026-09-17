package com.tictactoe.listener;

import com.tictactoe.service.MatchmakingService;
import com.tictactoe.service.MultiplayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Event listener for WebSocket disconnects.
 * Cleans up matchmaking queue and handles active match abandonment.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final MatchmakingService matchmakingService;
    private final MultiplayerService multiplayerService;

    @Autowired
    public WebSocketEventListener(MatchmakingService matchmakingService, MultiplayerService multiplayerService) {
        this.matchmakingService = matchmakingService;
        this.multiplayerService = multiplayerService;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal user = headerAccessor.getUser();
        String principalName = (user != null) ? user.getName() : null;

        log.info("WebSocket disconnected. SessionId: {}, Principal: {}", sessionId, principalName);

        // Clean up matchmaking queue
        matchmakingService.removeBySessionId(sessionId);

        // Clean up or abandon active game session
        multiplayerService.handlePlayerDisconnect(sessionId, principalName);
    }
}
