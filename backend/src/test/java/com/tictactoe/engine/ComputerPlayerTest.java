package com.tictactoe.engine;

import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComputerPlayerTest {

    private GameEngine gameEngine;
    private ComputerPlayer computerPlayer;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
        computerPlayer = new ComputerPlayer(gameEngine);
    }

    @Nested
    @DisplayName("Medium Strategy - Rule-based AI Concrete Scenarios")
    class MediumStrategyTests {

        @Test
        @DisplayName("Scenario A: Computer can win immediately (selects position 3 / index 2)")
        void shouldExecuteScenarioA_ComputerWins() {
            // O | O | - (position 3 / index 2 is winning for O)
            // X | X | -
            // - | - | -
            Board board = createBoard(
                "O", "O", "",
                "X", "X", "",
                "",  "",  ""
            );
            int chosenIndex = computerPlayer.chooseMove(board, Difficulty.MEDIUM);
            assertEquals(2, chosenIndex, "Computer should select position 3 (index 2) to win.");
        }

        @Test
        @DisplayName("Scenario B: Player must be blocked (selects position 3 / index 2)")
        void shouldExecuteScenarioB_BlockPlayer() {
            // X | X | - (position 3 / index 2 threatens win for X)
            // O | - | -
            // - | - | O
            Board board = createBoard(
                "X", "X", "",
                "O", "",  "",
                "",  "",  "O"
            );
            int chosenIndex = computerPlayer.chooseMove(board, Difficulty.MEDIUM);
            assertEquals(2, chosenIndex, "Computer should select position 3 (index 2) to block player.");
        }

        @Test
        @DisplayName("Scenario C: Neither can immediately win (selects any available empty cell)")
        void shouldExecuteScenarioC_FallbackToRandom() {
            // X | - | -
            // - | O | -
            // - | - | -
            Board board = createBoard(
                "X", "", "",
                "", "O", "",
                "", "",  ""
            );
            int chosenIndex = computerPlayer.chooseMove(board, Difficulty.MEDIUM);
            assertTrue(board.isCellEmpty(chosenIndex), "Selected fallback move must be an empty cell.");
            assertNotEquals(0, chosenIndex, "Cannot pick index 0 (occupied by X).");
            assertNotEquals(4, chosenIndex, "Cannot pick index 4 (occupied by O).");
        }

        @Test
        @DisplayName("Scenario D: Both can win (Priority 1 WIN > Priority 2 BLOCK)")
        void shouldExecuteScenarioD_PrioritizeWinOverBlock() {
            // O | O | - (position 3 / index 2 is winning for O)
            // X | X | - (position 6 / index 5 is winning for X)
            // - | - | -
            Board board = createBoard(
                "O", "O", "",
                "X", "X", "",
                "",  "",  ""
            );
            int chosenIndex = computerPlayer.chooseMove(board, Difficulty.MEDIUM);
            assertEquals(2, chosenIndex, "Computer must take its own winning move (index 2) instead of blocking index 5.");
        }
    }

    @Nested
    @DisplayName("Easy Strategy - Pure Random AI")
    class EasyStrategyTests {

        @Test
        @DisplayName("Easy AI always selects an available empty cell")
        void shouldAlwaysSelectAnEmptyCell() {
            Board board = createBoard(
                "X", "O", "X",
                "O", "X", "",
                "",  "",  ""
            );
            Set<Integer> validIndices = new HashSet<>(Arrays.asList(5, 6, 7, 8));

            for (int i = 0; i < 50; i++) {
                int chosen = computerPlayer.chooseMove(board, Difficulty.EASY);
                assertTrue(validIndices.contains(chosen), "Chosen index " + chosen + " must be one of the empty cells.");
                assertTrue(board.isCellEmpty(chosen));
            }
        }

        @Test
        @DisplayName("Easy AI never overwrites existing marks")
        void shouldNeverOverwriteExistingMarks() {
            Board board = createBoard(
                "X", "O", "X",
                "",  "X", "",
                "",  "",  "O"
            );
            Set<Integer> occupiedIndices = Set.of(0, 1, 2, 4, 8);

            for (int i = 0; i < 30; i++) {
                int chosen = computerPlayer.chooseMove(board, Difficulty.EASY);
                assertFalse(occupiedIndices.contains(chosen), "Easy AI must never select an occupied index: " + chosen);
            }
        }

        @Test
        @DisplayName("Easy AI with controlled seed selects predictable empty position")
        void shouldSelectUsingInjectedRandom() {
            Board board = createBoard(
                "X", "", "",
                "",  "", "",
                "",  "", ""
            );
            // Injected random to verify testability
            Random deterministicRandom = new Random(42);
            ComputerPlayer deterministicPlayer = new ComputerPlayer(gameEngine, deterministicRandom);

            int move = deterministicPlayer.chooseEasyMove(board);
            assertTrue(board.isCellEmpty(move));
        }

        @Test
        @DisplayName("Throws IllegalStateException when board is full")
        void shouldThrowExceptionWhenBoardIsFull() {
            Board fullBoard = createBoard(
                "X", "O", "X",
                "X", "O", "O",
                "O", "X", "X"
            );
            assertThrows(IllegalStateException.class, () -> computerPlayer.chooseEasyMove(fullBoard));
        }
    }

    @Nested
    @DisplayName("Hard Strategy - Deferred to Phase 2")
    class HardStrategyTests {

        @Test
        @DisplayName("Throws UnsupportedOperationException when Hard difficulty is selected")
        void shouldThrowWhenHardDifficultyRequested() {
            Board board = createBoard(
                "X", "", "",
                "",  "", "",
                "",  "", ""
            );
            UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> computerPlayer.chooseMove(board, Difficulty.HARD)
            );
            assertTrue(exception.getMessage().contains("Phase 2"));
        }
    }

    private static Board createBoard(String... cells) {
        return new Board(Arrays.asList(cells));
    }
}
