package com.tictactoe.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    @DisplayName("GameMode enum constants exist")
    void testGameModeEnum() {
        assertEquals(2, GameMode.values().length);
        assertNotNull(GameMode.valueOf("COMPUTER"));
        assertNotNull(GameMode.valueOf("MULTIPLAYER"));
    }

    @Test
    @DisplayName("PlayerSymbol methods and resolution")
    void testPlayerSymbol() {
        assertEquals("X", PlayerSymbol.X.getValue());
        assertEquals("O", PlayerSymbol.O.getValue());
        assertEquals(PlayerSymbol.O, PlayerSymbol.X.opponent());
        assertEquals(PlayerSymbol.X, PlayerSymbol.O.opponent());

        assertEquals(PlayerSymbol.X, PlayerSymbol.fromString("x"));
        assertEquals(PlayerSymbol.O, PlayerSymbol.fromString("o"));

        assertThrows(IllegalArgumentException.class, () -> PlayerSymbol.fromString("Z"));
        assertThrows(IllegalArgumentException.class, () -> PlayerSymbol.fromString(null));
    }

    @Test
    @DisplayName("Difficulty enum contains EASY, MEDIUM, and HARD")
    void testDifficultyEnum() {
        assertEquals(3, Difficulty.values().length);
        assertEquals(Difficulty.EASY, Difficulty.valueOf("EASY"));
        assertEquals(Difficulty.MEDIUM, Difficulty.valueOf("MEDIUM"));
        assertEquals(Difficulty.HARD, Difficulty.valueOf("HARD"));
    }

    @Test
    @DisplayName("GameStatus terminal states validation")
    void testGameStatusTerminalChecks() {
        assertFalse(GameStatus.WAITING.isTerminal());
        assertFalse(GameStatus.MATCHED.isTerminal());
        assertFalse(GameStatus.IN_PROGRESS.isTerminal());

        assertTrue(GameStatus.PLAYER_WON.isTerminal());
        assertTrue(GameStatus.COMPUTER_WON.isTerminal());
        assertTrue(GameStatus.PLAYER_ONE_WON.isTerminal());
        assertTrue(GameStatus.PLAYER_TWO_WON.isTerminal());
        assertTrue(GameStatus.DRAW.isTerminal());
        assertTrue(GameStatus.ABANDONED.isTerminal());
    }
}
