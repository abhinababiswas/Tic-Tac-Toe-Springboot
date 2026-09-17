package com.tictactoe.controller;

import com.tictactoe.dto.websocket.JoinMatchmakingRequest;
import com.tictactoe.dto.websocket.LeaveMatchmakingRequest;
import com.tictactoe.dto.websocket.MultiplayerMoveRequest;
import com.tictactoe.dto.websocket.WebSocketErrorMessage;
import com.tictactoe.exception.InvalidMoveException;
import com.tictactoe.service.MatchmakingService;
import com.tictactoe.service.MultiplayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

/**
 * Thin WebSocket / STOMP message controller.
 * Routes client actions to MatchmakingService and MultiplayerService.
 */
@Controller
public class MultiplayerWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(MultiplayerWebSocketController.class);

    private final MatchmakingService matchmakingService;
    private final MultiplayerService multiplayerService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MultiplayerWebSocketController(MatchmakingService matchmakingService,
                                          MultiplayerService multiplayerService,
                                          SimpMessagingTemplate messagingTemplate) {
        this.matchmakingService = matchmakingService;
        this.multiplayerService = multiplayerService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Enqueues a player in matchmaking. If paired with an available opponent,
     * immediately starts an authoritative multiplayer game session.
     */
    @MessageMapping("/matchmaking/join")
    public void joinMatchmaking(@Payload JoinMatchmakingRequest request,
                                Principal principal,
                                SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String principalName = (principal != null) ? principal.getName() : sessionId;

        if (request == null || request.getUserId() == null || request.getUsername() == null || request.getUsername().isBlank()) {
            sendError(principalName, "INVALID_REQUEST", "User ID and valid username are required to join matchmaking.");
            return;
        }

        try {
            Optional<MatchmakingService.MatchPair> matchOpt = matchmakingService.enqueuePlayer(
                    request.getUserId(),
                    request.getUsername().trim(),
                    sessionId,
                    principalName
            );

            if (matchOpt.isPresent()) {
                MatchmakingService.MatchPair pair = matchOpt.get();
                multiplayerService.createGameSession(pair.player1(), pair.player2());
            }
        } catch (IllegalStateException e) {
            sendError(principalName, "ALREADY_IN_MATCHMAKING", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in joinMatchmaking: {}", e.getMessage(), e);
            sendError(principalName, "INTERNAL_ERROR", "An unexpected error occurred during matchmaking.");
        }
    }

    /**
     * Removes a player from the matchmaking queue.
     */
    @MessageMapping("/matchmaking/leave")
    public void leaveMatchmaking(@Payload LeaveMatchmakingRequest request,
                                 Principal principal,
                                 SimpMessageHeaderAccessor headerAccessor) {
        if (request != null && request.getUserId() != null) {
            matchmakingService.leaveQueue(request.getUserId());
        }
    }

    /**
     * Applies a player's move in an active multiplayer game session.
     */
    @MessageMapping("/game/{gameId}/move")
    public void makeMove(@DestinationVariable String gameId,
                         @Payload MultiplayerMoveRequest request,
                         Principal principal,
                         SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String principalName = (principal != null) ? principal.getName() : sessionId;

        if (request == null || request.getUserId() == null) {
            sendError(principalName, "INVALID_MOVE", "User ID must not be null.");
            return;
        }

        try {
            multiplayerService.processMove(gameId, request.getUserId(), request.getPosition());
        } catch (InvalidMoveException e) {
            sendError(principalName, "INVALID_MOVE", e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(principalName, "MATCH_NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in makeMove for game {}: {}", gameId, e.getMessage(), e);
            sendError(principalName, "INTERNAL_GAME_ERROR", "Failed to process move.");
        }
    }

    private void sendError(String targetUser, String code, String message) {
        WebSocketErrorMessage errorMessage = new WebSocketErrorMessage(code, message);
        if (targetUser != null) {
            messagingTemplate.convertAndSendToUser(targetUser, "/queue/errors", errorMessage);
        }
    }
}
