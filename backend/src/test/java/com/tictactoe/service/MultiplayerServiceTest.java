package com.tictactoe.service;

import com.tictactoe.dto.websocket.GameStateMessage;
import com.tictactoe.dto.websocket.MatchFoundMessage;
import com.tictactoe.dto.websocket.PlayerDisconnectedMessage;
import com.tictactoe.engine.GameEngine;
import com.tictactoe.model.GameStatus;
import com.tictactoe.model.MultiplayerGameSession;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.model.WaitingPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MultiplayerServiceTest {

    private SimpMessagingTemplate messagingTemplate;
    private GameEngine gameEngine;
    private GameHistoryService gameHistoryService;
    private MultiplayerService multiplayerService;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        gameEngine = new GameEngine();
        gameHistoryService = mock(GameHistoryService.class);
        multiplayerService = new MultiplayerService(messagingTemplate, gameEngine, gameHistoryService);
    }

    @Test
    @DisplayName("createGameSession pairs players, assigns X and O, and sends initial notifications")
    void testCreateGameSession() {
        WaitingPlayer p1 = new WaitingPlayer(1L, "Alice", "sess-1", "user-1");
        WaitingPlayer p2 = new WaitingPlayer(2L, "Bob", "sess-2", "user-2");

        MultiplayerGameSession session = multiplayerService.createGameSession(p1, p2);

        assertNotNull(session.getGameId());
        assertNotNull(session.getPlayerX());
        assertNotNull(session.getPlayerO());
        assertNotEquals(session.getPlayerX().getUserId(), session.getPlayerO().getUserId());

        // Verify notifications sent to user queues
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/match"), any(MatchFoundMessage.class));

        // Verify broadcast to game topic
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/" + session.getGameId()), any(GameStateMessage.class));
    }

    @Test
    @DisplayName("processMove executes valid move and broadcasts updated game state")
    void testProcessMoveInProgress() {
        WaitingPlayer p1 = new WaitingPlayer(1L, "Alice", "sess-1", "user-1");
        WaitingPlayer p2 = new WaitingPlayer(2L, "Bob", "sess-2", "user-2");

        MultiplayerGameSession session = multiplayerService.createGameSession(p1, p2);
        String gameId = session.getGameId();
        Long playerXId = session.getPlayerX().getUserId();

        GameStateMessage message = multiplayerService.processMove(gameId, playerXId, 5);

        assertEquals("GAME_UPDATE", message.getType());
        assertEquals(GameStatus.IN_PROGRESS, message.getStatus());
        assertEquals(PlayerSymbol.O, message.getCurrentTurn());
        assertEquals(5, message.getLastMovePosition());
        assertEquals(PlayerSymbol.X, message.getLastMovePlayer());

        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/game/" + gameId), any(GameStateMessage.class));
        verify(gameHistoryService, never()).recordCompletedMultiplayerGame(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("processMove on winning move persists game and broadcasts GAME_FINISHED")
    void testProcessMoveWinningGamePersists() {
        WaitingPlayer p1 = new WaitingPlayer(1L, "Alice", "sess-1", "user-1");
        WaitingPlayer p2 = new WaitingPlayer(2L, "Bob", "sess-2", "user-2");

        MultiplayerGameSession session = multiplayerService.createGameSession(p1, p2);
        String gameId = session.getGameId();
        Long xId = session.getPlayerX().getUserId();
        Long oId = session.getPlayerO().getUserId();

        // X: 1, 2, 3 (wins)
        // O: 4, 5
        multiplayerService.processMove(gameId, xId, 1);
        multiplayerService.processMove(gameId, oId, 4);
        multiplayerService.processMove(gameId, xId, 2);
        multiplayerService.processMove(gameId, oId, 5);
        GameStateMessage finalState = multiplayerService.processMove(gameId, xId, 3); // X wins

        assertEquals("GAME_FINISHED", finalState.getType());
        assertEquals(GameStatus.PLAYER_ONE_WON, finalState.getStatus());
        assertEquals(PlayerSymbol.X, finalState.getWinner());

        // Verify persistence was called for the terminal game
        verify(gameHistoryService, times(1)).recordCompletedMultiplayerGame(
                eq(xId), eq(oId), eq(GameStatus.PLAYER_ONE_WON), any(), any()
        );
    }

    @Test
    @DisplayName("handlePlayerDisconnect marks active game ABANDONED and notifies opponent")
    void testDisconnectMarksAbandoned() {
        WaitingPlayer p1 = new WaitingPlayer(1L, "Alice", "sess-1", "user-1");
        WaitingPlayer p2 = new WaitingPlayer(2L, "Bob", "sess-2", "user-2");

        MultiplayerGameSession session = multiplayerService.createGameSession(p1, p2);
        String gameId = session.getGameId();

        multiplayerService.handlePlayerDisconnect("sess-1", "user-1");

        assertEquals(GameStatus.ABANDONED, session.getStatus());

        ArgumentCaptor<PlayerDisconnectedMessage> captor = ArgumentCaptor.forClass(PlayerDisconnectedMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameId), captor.capture());
        assertTrue(captor.getValue().getMessage().contains("Opponent disconnected"));
    }
}
