package com.tictactoe.service;

import com.tictactoe.dto.websocket.GameStateMessage;
import com.tictactoe.dto.websocket.MatchFoundMessage;
import com.tictactoe.dto.websocket.PlayerDisconnectedMessage;
import com.tictactoe.engine.GameEngine;
import com.tictactoe.model.GameStatus;
import com.tictactoe.model.MultiplayerGameSession;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.model.WaitingPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service orchestrating active multiplayer game sessions, real-time moves,
 * state broadcasts, disconnect handling, and terminal persistence.
 */
@Service
public class MultiplayerService {

    private static final Logger log = LoggerFactory.getLogger(MultiplayerService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final GameEngine gameEngine;
    private final GameHistoryService gameHistoryService;

    // Active in-memory game sessions (instance-local architecture)
    private final ConcurrentHashMap<String, MultiplayerGameSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToGameId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> principalToGameId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userToGameId = new ConcurrentHashMap<>();

    @Autowired
    public MultiplayerService(SimpMessagingTemplate messagingTemplate,
                              GameEngine gameEngine,
                              GameHistoryService gameHistoryService) {
        this.messagingTemplate = messagingTemplate;
        this.gameEngine = gameEngine;
        this.gameHistoryService = gameHistoryService;
    }

    /**
     * Pairs two players into a new multiplayer game session.
     * Randomly assigns 'X' and 'O', registers session maps, notifies players, and broadcasts start state.
     */
    public MultiplayerGameSession createGameSession(WaitingPlayer p1, WaitingPlayer p2) {
        // Server-authoritative random symbol assignment
        boolean p1IsX = ThreadLocalRandom.current().nextBoolean();
        WaitingPlayer playerX = p1IsX ? p1 : p2;
        WaitingPlayer playerO = p1IsX ? p2 : p1;

        String gameId = UUID.randomUUID().toString();
        MultiplayerGameSession session = new MultiplayerGameSession(gameId, playerX, playerO);

        activeSessions.put(gameId, session);

        // Index identifiers for disconnect and lookup
        if (playerX.getStompSessionId() != null) sessionToGameId.put(playerX.getStompSessionId(), gameId);
        if (playerO.getStompSessionId() != null) sessionToGameId.put(playerO.getStompSessionId(), gameId);
        if (playerX.getPrincipalName() != null) principalToGameId.put(playerX.getPrincipalName(), gameId);
        if (playerO.getPrincipalName() != null) principalToGameId.put(playerO.getPrincipalName(), gameId);
        if (playerX.getUserId() != null) userToGameId.put(playerX.getUserId(), gameId);
        if (playerO.getUserId() != null) userToGameId.put(playerO.getUserId(), gameId);

        log.info("Created multiplayer game {} with Player X: {} ({}) and Player O: {} ({})",
                gameId, playerX.getUsername(), playerX.getUserId(),
                playerO.getUsername(), playerO.getUserId());

        // Notify Player X
        MatchFoundMessage matchX = new MatchFoundMessage(
                gameId,
                PlayerSymbol.X,
                playerO.getUsername(),
                session.getCurrentTurn(),
                session.getBoard().getCells(),
                session.getStatus()
        );
        sendMatchNotification(playerX, matchX);

        // Notify Player O
        MatchFoundMessage matchO = new MatchFoundMessage(
                gameId,
                PlayerSymbol.O,
                playerX.getUsername(),
                session.getCurrentTurn(),
                session.getBoard().getCells(),
                session.getStatus()
        );
        sendMatchNotification(playerO, matchO);

        // Broadcast game start to room
        GameStateMessage startMessage = new GameStateMessage(
                "GAME_START",
                gameId,
                session.getBoard().getCells(),
                session.getCurrentTurn(),
                session.getStatus(),
                "Game started! " + playerX.getUsername() + " (X) goes first.",
                null,
                null,
                null,
                null
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, startMessage);

        return session;
    }

    /**
     * Processes a move in an active game session under synchronized session lock.
     */
    public GameStateMessage processMove(String gameId, Long userId, int position) {
        MultiplayerGameSession session = activeSessions.get(gameId);
        if (session == null) {
            throw new IllegalArgumentException("MATCH_NOT_FOUND: No active game found with ID " + gameId);
        }

        // Apply move under session lock
        session.applyMove(userId, position, gameEngine);

        GameStateMessage responseMessage;

        if (session.getStatus() == GameStatus.PLAYER_ONE_WON || session.getStatus() == GameStatus.PLAYER_TWO_WON) {
            // Persist terminal game record
            recordCompletedGame(session);

            String winnerName = (session.getWinner() == PlayerSymbol.X)
                    ? session.getPlayerX().getUsername()
                    : session.getPlayerO().getUsername();

            responseMessage = new GameStateMessage(
                    "GAME_FINISHED",
                    gameId,
                    session.getBoard().getCells(),
                    session.getCurrentTurn(),
                    session.getStatus(),
                    "Player " + winnerName + " (" + session.getWinner() + ") won the game!",
                    session.getLastMovePosition(),
                    session.getLastMovePlayer(),
                    session.getWinner(),
                    session.getWinningLine()
            );
        } else if (session.getStatus() == GameStatus.DRAW) {
            // Persist terminal game record
            recordCompletedGame(session);

            responseMessage = new GameStateMessage(
                    "GAME_FINISHED",
                    gameId,
                    session.getBoard().getCells(),
                    session.getCurrentTurn(),
                    session.getStatus(),
                    "Game ended in a draw!",
                    session.getLastMovePosition(),
                    session.getLastMovePlayer(),
                    null,
                    null
            );
        } else {
            // Game in progress
            String nextPlayer = (session.getCurrentTurn() == PlayerSymbol.X)
                    ? session.getPlayerX().getUsername()
                    : session.getPlayerO().getUsername();

            responseMessage = new GameStateMessage(
                    "GAME_UPDATE",
                    gameId,
                    session.getBoard().getCells(),
                    session.getCurrentTurn(),
                    session.getStatus(),
                    "Move accepted. " + nextPlayer + "'s turn (" + session.getCurrentTurn() + ").",
                    session.getLastMovePosition(),
                    session.getLastMovePlayer(),
                    null,
                    null
            );
        }

        // Authoritative state broadcast to all participants
        messagingTemplate.convertAndSend("/topic/game/" + gameId, responseMessage);

        return responseMessage;
    }

    /**
     * Handles player disconnect. If in active match, marks match as ABANDONED and notifies opponent.
     */
    public void handlePlayerDisconnect(String stompSessionId, String principalName) {
        String gameId = null;
        if (stompSessionId != null) {
            gameId = sessionToGameId.remove(stompSessionId);
        }
        if (gameId == null && principalName != null) {
            gameId = principalToGameId.remove(principalName);
        }

        if (gameId != null) {
            MultiplayerGameSession session = activeSessions.get(gameId);
            if (session != null && session.markAbandoned()) {
                log.info("Player disconnected from active game {}. Match marked ABANDONED.", gameId);

                PlayerDisconnectedMessage disconnectMsg = new PlayerDisconnectedMessage(
                        gameId,
                        "Opponent disconnected. The game has been marked as abandoned."
                );
                messagingTemplate.convertAndSend("/topic/game/" + gameId, disconnectMsg);

                GameStateMessage stateMsg = new GameStateMessage(
                        "GAME_ABANDONED",
                        gameId,
                        session.getBoard().getCells(),
                        session.getCurrentTurn(),
                        session.getStatus(),
                        "Game abandoned due to player disconnect.",
                        session.getLastMovePosition(),
                        session.getLastMovePlayer(),
                        null,
                        null
                );
                messagingTemplate.convertAndSend("/topic/game/" + gameId, stateMsg);
            }
        }
    }

    private void recordCompletedGame(MultiplayerGameSession session) {
        try {
            Long xId = session.getPlayerX().getUserId();
            Long oId = session.getPlayerO().getUserId();

            gameHistoryService.recordCompletedMultiplayerGame(
                    xId,
                    oId,
                    session.getStatus(),
                    session.getStartedAt(),
                    session.getCompletedAt()
            );
            log.info("Successfully persisted completed multiplayer game {}", session.getGameId());
        } catch (Exception e) {
            log.error("Failed to persist completed multiplayer game {}: {}", session.getGameId(), e.getMessage(), e);
        }
    }

    private void sendMatchNotification(WaitingPlayer player, MatchFoundMessage msg) {
        // Send to targeted user destination (/user/queue/match)
        if (player.getPrincipalName() != null) {
            messagingTemplate.convertAndSendToUser(player.getPrincipalName(), "/queue/match", msg);
        } else if (player.getStompSessionId() != null) {
            messagingTemplate.convertAndSendToUser(player.getStompSessionId(), "/queue/match", msg);
        }

        // Secondary fallback destination (/topic/match/{userId}) for testing flexibility
        if (player.getUserId() != null) {
            messagingTemplate.convertAndSend("/topic/match/" + player.getUserId(), msg);
        }
    }

    public MultiplayerGameSession getSession(String gameId) {
        return activeSessions.get(gameId);
    }

    public void clear() {
        activeSessions.clear();
        sessionToGameId.clear();
        principalToGameId.clear();
        userToGameId.clear();
    }
}
