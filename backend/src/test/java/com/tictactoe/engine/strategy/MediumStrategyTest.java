package com.tictactoe.engine.strategy;

import com.tictactoe.engine.GameEngine;
import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MediumStrategyTest {

    private GameEngine gameEngine;
    private MediumStrategy mediumStrategy;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
        mediumStrategy = new MediumStrategy(gameEngine, new EasyStrategy());
    }

    @Test
    @DisplayName("Returns Difficulty.MEDIUM")
    void shouldReturnMediumDifficulty() {
        assertEquals(Difficulty.MEDIUM, mediumStrategy.getDifficulty());
    }

    @Test
    @DisplayName("Takes immediate winning move for computer ('O')")
    void shouldTakeImmediateWinningMove() {
        // O | O | -  (index 2 wins for O)
        // X | X | -
        // - | - | -
        Board board = new Board(Arrays.asList(
            "O", "O", "",
            "X", "X", "",
            "",  "",  ""
        ));
        int move = mediumStrategy.chooseMove(board);
        assertEquals(2, move, "MediumStrategy must choose winning move at index 2.");
    }

    @Test
    @DisplayName("Blocks immediate player ('X') win")
    void shouldBlockImmediatePlayerWin() {
        // X | X | -  (index 2 wins for X)
        // O | - | -
        // - | - | O
        Board board = new Board(Arrays.asList(
            "X", "X", "",
            "O", "",  "",
            "",  "",  "O"
        ));
        int move = mediumStrategy.chooseMove(board);
        assertEquals(2, move, "MediumStrategy must block player win at index 2.");
    }

    @Test
    @DisplayName("Prioritizes winning over blocking when both exist")
    void shouldPrioritizeWinningOverBlocking() {
        // O | O | -  (index 2 wins for O)
        // X | X | -  (index 5 wins for X)
        // - | - | -
        Board board = new Board(Arrays.asList(
            "O", "O", "",
            "X", "X", "",
            "",  "",  ""
        ));
        int move = mediumStrategy.chooseMove(board);
        assertEquals(2, move, "MediumStrategy must prioritize winning over blocking.");
    }

    @Test
    @DisplayName("Falls back to empty cell when no immediate win or block exists")
    void shouldFallbackToEmptyCell() {
        // X | - | -
        // - | O | -
        // - | - | -
        Board board = new Board(Arrays.asList(
            "X", "", "",
            "",  "O", "",
            "",  "",  ""
        ));
        int move = mediumStrategy.chooseMove(board);
        assertTrue(board.isCellEmpty(move));
    }
}
