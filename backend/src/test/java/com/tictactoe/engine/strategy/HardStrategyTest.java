package com.tictactoe.engine.strategy;

import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HardStrategyTest {

    @Test
    @DisplayName("Returns Difficulty.HARD")
    void shouldReturnHardDifficulty() {
        HardStrategy strategy = new HardStrategy();
        assertEquals(Difficulty.HARD, strategy.getDifficulty());
    }

    @Test
    @DisplayName("Throws UnsupportedOperationException explaining Phase 2 deferral")
    void shouldThrowUnsupportedOperationException() {
        HardStrategy strategy = new HardStrategy();
        Board board = new Board();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> strategy.chooseMove(board)
        );
        assertTrue(exception.getMessage().contains("Phase 2"));
    }
}
