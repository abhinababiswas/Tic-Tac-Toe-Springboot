package com.tictactoe.engine.strategy;

import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EasyStrategyTest {

    @Test
    @DisplayName("Returns Difficulty.EASY")
    void shouldReturnEasyDifficulty() {
        EasyStrategy strategy = new EasyStrategy();
        assertEquals(Difficulty.EASY, strategy.getDifficulty());
    }

    @Test
    @DisplayName("Picks a valid empty cell from empty board")
    void shouldPickEmptyCellFromEmptyBoard() {
        EasyStrategy strategy = new EasyStrategy();
        Board board = new Board();
        int move = strategy.chooseMove(board);
        assertTrue(move >= 0 && move <= 8);
        assertTrue(board.isCellEmpty(move));
    }

    @Test
    @DisplayName("Picks the only remaining empty cell on almost full board")
    void shouldPickSoleEmptyCell() {
        EasyStrategy strategy = new EasyStrategy();
        Board board = new Board(Arrays.asList(
            "X", "O", "X",
            "O", "X", "O",
            "O", "X", ""
        ));
        int move = strategy.chooseMove(board);
        assertEquals(8, move);
    }

    @Test
    @DisplayName("Throws IllegalStateException when board is full")
    void shouldThrowWhenBoardIsFull() {
        EasyStrategy strategy = new EasyStrategy();
        Board board = new Board(Arrays.asList(
            "X", "O", "X",
            "O", "X", "O",
            "O", "X", "O"
        ));
        assertThrows(IllegalStateException.class, () -> strategy.chooseMove(board));
    }

    @Test
    @DisplayName("Demonstrates pseudo-random distribution across available cells")
    void shouldDistributeRandomlyAcrossAvailableCells() {
        EasyStrategy strategy = new EasyStrategy(new Random(42));
        Board board = new Board(Arrays.asList(
            "X", "O", "",
            "",  "X", "",
            "O", "",  ""
        ));
        // Empty positions: 2, 3, 5, 7, 8
        Set<Integer> observedIndices = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            observedIndices.add(strategy.chooseMove(board));
        }

        assertTrue(observedIndices.size() > 1, "Random strategy should explore multiple cells over repeated iterations.");
        assertTrue(board.getEmptyIndices().containsAll(observedIndices), "All chosen moves must be among empty cells.");
    }
}
