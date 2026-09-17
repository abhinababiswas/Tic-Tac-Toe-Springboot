package com.tictactoe.model;

import com.tictactoe.engine.GameEngine;
import com.tictactoe.exception.InvalidMoveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerGameSessionTest {

    private GameEngine gameEngine;
    private WaitingPlayer playerX;
    private WaitingPlayer playerO;
    private MultiplayerGameSession session;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
        playerX = new WaitingPlayer(10L, "Alice", "sess-x", "user-x");
        playerO = new WaitingPlayer(20L, "Bob", "sess-o", "user-o");
        session = new MultiplayerGameSession("game-123", playerX, playerO);
    }

    @Test
    @DisplayName("Initial session state is IN_PROGRESS with turn X and empty board")
    void testInitialState() {
        assertEquals("game-123", session.getGameId());
        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
        assertEquals(PlayerSymbol.X, session.getCurrentTurn());
        assertNotNull(session.getStartedAt());
        assertNull(session.getCompletedAt());
        assertNull(session.getWinner());

        for (int i = 0; i < 9; i++) {
            assertEquals("", session.getBoard().getCell(i));
        }
    }

    @Test
    @DisplayName("Player X makes first move, advances turn to Player O")
    void testValidMoveAlternatesTurn() {
        session.applyMove(10L, 5, gameEngine); // Center

        assertEquals("X", session.getBoard().getCell(4));
        assertEquals(PlayerSymbol.O, session.getCurrentTurn());
        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
        assertEquals(5, session.getLastMovePosition());
        assertEquals(PlayerSymbol.X, session.getLastMovePlayer());

        // Player O moves
        session.applyMove(20L, 1, gameEngine); // Top-left

        assertEquals("O", session.getBoard().getCell(0));
        assertEquals(PlayerSymbol.X, session.getCurrentTurn());
        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
    }

    @Test
    @DisplayName("Player moving out of turn is rejected with InvalidMoveException")
    void testOutOfTurnRejected() {
        // Player O tries to move first
        InvalidMoveException ex = assertThrows(
                InvalidMoveException.class,
                () -> session.applyMove(20L, 1, gameEngine)
        );

        assertTrue(ex.getMessage().contains("It is not your turn"));
        assertEquals(PlayerSymbol.X, session.getCurrentTurn());
    }

    @Test
    @DisplayName("Targeting an occupied cell is rejected")
    void testOccupiedCellRejected() {
        session.applyMove(10L, 5, gameEngine);

        InvalidMoveException ex = assertThrows(
                InvalidMoveException.class,
                () -> session.applyMove(20L, 5, gameEngine)
        );

        assertTrue(ex.getMessage().contains("already occupied"));
        assertEquals(PlayerSymbol.O, session.getCurrentTurn());
    }

    @Test
    @DisplayName("Non-participant attempting move is rejected")
    void testNonParticipantRejected() {
        InvalidMoveException ex = assertThrows(
                InvalidMoveException.class,
                () -> session.applyMove(999L, 1, gameEngine)
        );

        assertTrue(ex.getMessage().contains("not a participant"));
    }

    @Test
    @DisplayName("Player X winning row is detected authoritatively")
    void testPlayerXWins() {
        // X: 1, 2, 3 (top row)
        // O: 4, 5
        session.applyMove(10L, 1, gameEngine); // X
        session.applyMove(20L, 4, gameEngine); // O
        session.applyMove(10L, 2, gameEngine); // X
        session.applyMove(20L, 5, gameEngine); // O
        session.applyMove(10L, 3, gameEngine); // X wins!

        assertEquals(GameStatus.PLAYER_ONE_WON, session.getStatus());
        assertEquals(PlayerSymbol.X, session.getWinner());
        assertNotNull(session.getCompletedAt());
        assertEquals(List.of(1, 2, 3), session.getWinningLine());

        // Subsequent move must be rejected
        assertThrows(
                InvalidMoveException.class,
                () -> session.applyMove(20L, 6, gameEngine)
        );
    }

    @Test
    @DisplayName("Draw game is correctly identified when all 9 cells are filled")
    void testDrawGame() {
        // Board layout for draw:
        // X O X
        // X O O
        // O X X
        // Moves:
        // 1(X), 2(O), 3(X)
        // 5(O), 4(X), 6(O)
        // 8(X), 7(O), 9(X)
        session.applyMove(10L, 1, gameEngine); // X
        session.applyMove(20L, 2, gameEngine); // O
        session.applyMove(10L, 3, gameEngine); // X
        session.applyMove(20L, 5, gameEngine); // O
        session.applyMove(10L, 4, gameEngine); // X
        session.applyMove(20L, 6, gameEngine); // O
        session.applyMove(10L, 8, gameEngine); // X
        session.applyMove(20L, 7, gameEngine); // O
        session.applyMove(10L, 9, gameEngine); // X

        assertEquals(GameStatus.DRAW, session.getStatus());
        assertNull(session.getWinner());
        assertNotNull(session.getCompletedAt());
    }

    @Test
    @DisplayName("Marking abandoned transitions IN_PROGRESS game to ABANDONED")
    void testMarkAbandoned() {
        assertTrue(session.markAbandoned());
        assertEquals(GameStatus.ABANDONED, session.getStatus());
        assertNotNull(session.getCompletedAt());

        // Calling markAbandoned on an already abandoned game returns false
        assertFalse(session.markAbandoned());
    }
}
