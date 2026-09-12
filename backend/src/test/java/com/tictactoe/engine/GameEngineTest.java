package com.tictactoe.engine;

import com.tictactoe.exception.InvalidBoardException;
import com.tictactoe.exception.InvalidMoveException;
import com.tictactoe.model.Board;
import com.tictactoe.model.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
    }

    @Nested
    @DisplayName("Winner Detection - All 8 Combinations")
    class WinnerDetectionTests {

        @ParameterizedTest(name = "Symbol {0} wins on Top Row")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnTopRow(String symbol) {
            // Positions 1, 2, 3 -> Indices 0, 1, 2
            Board board = createBoard(
                symbol, symbol, symbol,
                "",     "",     "",
                "",     "",     ""
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
            assertEquals(symbol.equals("X") ? GameStatus.PLAYER_WON : GameStatus.COMPUTER_WON, gameEngine.determineStatus(board));
        }

        @ParameterizedTest(name = "Symbol {0} wins on Middle Row")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnMiddleRow(String symbol) {
            // Positions 4, 5, 6 -> Indices 3, 4, 5
            Board board = createBoard(
                "",     "",     "",
                symbol, symbol, symbol,
                "",     "",     ""
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
        }

        @ParameterizedTest(name = "Symbol {0} wins on Bottom Row")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnBottomRow(String symbol) {
            // Positions 7, 8, 9 -> Indices 6, 7, 8
            Board board = createBoard(
                "",     "",     "",
                "",     "",     "",
                symbol, symbol, symbol
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
        }

        @ParameterizedTest(name = "Symbol {0} wins on Left Column")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnLeftColumn(String symbol) {
            // Positions 1, 4, 7 -> Indices 0, 3, 6
            Board board = createBoard(
                symbol, "", "",
                symbol, "", "",
                symbol, "", ""
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
        }

        @ParameterizedTest(name = "Symbol {0} wins on Middle Column")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnMiddleColumn(String symbol) {
            // Positions 2, 5, 8 -> Indices 1, 4, 7
            Board board = createBoard(
                "", symbol, "",
                "", symbol, "",
                "", symbol, ""
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
        }

        @ParameterizedTest(name = "Symbol {0} wins on Right Column")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnRightColumn(String symbol) {
            // Positions 3, 6, 9 -> Indices 2, 5, 8
            Board board = createBoard(
                "", "", symbol,
                "", "", symbol,
                "", "", symbol
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
        }

        @ParameterizedTest(name = "Symbol {0} wins on Top-Left to Bottom-Right Diagonal")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnMainDiagonal(String symbol) {
            // Positions 1, 5, 9 -> Indices 0, 4, 8
            Board board = createBoard(
                symbol, "",     "",
                "",     symbol, "",
                "",     "",     symbol
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
        }

        @ParameterizedTest(name = "Symbol {0} wins on Top-Right to Bottom-Left Diagonal")
        @ValueSource(strings = {"X", "O"})
        void shouldDetectWinOnAntiDiagonal(String symbol) {
            // Positions 3, 5, 7 -> Indices 2, 4, 6
            Board board = createBoard(
                "",     "",     symbol,
                "",     symbol, "",
                symbol, "",     ""
            );
            assertTrue(gameEngine.checkWinner(board, symbol));
        }

        @Test
        void shouldNotDetectWinWhenLineIsIncomplete() {
            Board board = createBoard(
                "X", "X", "",
                "O", "O", "",
                "",  "",  ""
            );
            assertFalse(gameEngine.checkWinner(board, "X"));
            assertFalse(gameEngine.checkWinner(board, "O"));
            assertTrue(gameEngine.getWinner(board).isEmpty());
        }
    }

    @Nested
    @DisplayName("Draw Detection")
    class DrawDetectionTests {

        @Test
        void shouldDetectDrawWhenBoardIsFullWithoutWinner() {
            // X O X
            // X O O
            // O X X
            Board board = createBoard(
                "X", "O", "X",
                "X", "O", "O",
                "O", "X", "X"
            );
            assertTrue(board.isFull());
            assertTrue(gameEngine.checkDraw(board));
            assertEquals(GameStatus.DRAW, gameEngine.determineStatus(board));
        }

        @Test
        void shouldNotClassifyWinningFullBoardAsDraw() {
            // X X X
            // O O X
            // O X O
            Board board = createBoard(
                "X", "X", "X",
                "O", "O", "X",
                "O", "X", "O"
            );
            assertTrue(board.isFull());
            assertFalse(gameEngine.checkDraw(board));
            assertTrue(gameEngine.checkWinner(board, "X"));
            assertEquals(GameStatus.PLAYER_WON, gameEngine.determineStatus(board));
        }

        @Test
        void shouldNotDetectDrawWhenEmptyCellsRemain() {
            Board board = createBoard(
                "X", "O", "X",
                "X", "",  "O",
                "O", "X", ""
            );
            assertFalse(gameEngine.checkDraw(board));
            assertEquals(GameStatus.IN_PROGRESS, gameEngine.determineStatus(board));
        }
    }

    @Nested
    @DisplayName("Winning Move Simulation")
    class WinningMoveSimulationTests {

        @Test
        void shouldDetectWinningMoveForSymbol() {
            // X X _
            // O _ _
            // _ _ _
            Board board = createBoard(
                "X", "X", "",
                "O", "",  "",
                "",  "",  ""
            );
            // Index 2 (Position 3) wins for X
            assertTrue(gameEngine.isWinningMove(board, 2, "X"));
            // But does not win for O
            assertFalse(gameEngine.isWinningMove(board, 2, "O"));
            // Index 4 (Position 5) is empty but does not win for X
            assertFalse(gameEngine.isWinningMove(board, 4, "X"));
        }

        @Test
        void shouldReturnFalseForOccupiedCell() {
            Board board = createBoard(
                "X", "X", "O",
                "",  "",  "",
                "",  "",  ""
            );
            assertFalse(gameEngine.isWinningMove(board, 2, "X"));
        }

        @Test
        void shouldFindWinningMoveIndex() {
            // O O _
            // X X _
            // _ _ _
            Board board = createBoard(
                "O", "O", "",
                "X", "X", "",
                "",  "",  ""
            );
            OptionalInt winningForO = gameEngine.findWinningMoveIndex(board, "O");
            assertTrue(winningForO.isPresent());
            assertEquals(2, winningForO.getAsInt());

            OptionalInt winningForX = gameEngine.findWinningMoveIndex(board, "X");
            assertTrue(winningForX.isPresent());
            assertEquals(5, winningForX.getAsInt());
        }

        @Test
        void shouldReturnEmptyWhenNoWinningMoveExists() {
            Board board = createBoard(
                "X", "", "",
                "",  "", "",
                "",  "", "O"
            );
            assertTrue(gameEngine.findWinningMoveIndex(board, "X").isEmpty());
            assertTrue(gameEngine.findWinningMoveIndex(board, "O").isEmpty());
        }
    }

    @Nested
    @DisplayName("Board & Move Validation")
    class ValidationTests {

        @Test
        void shouldPassValidationOnFreshBoard() {
            Board board = new Board();
            assertDoesNotThrow(() -> gameEngine.validateBoard(board));
        }

        @Test
        void shouldRejectBoardWithInconsistentTurnCount() {
            // 2 X's and 0 O's -> turn count inconsistent before player move
            Board board = createBoard(
                "X", "X", "",
                "",  "",  "",
                "",  "",  ""
            );
            assertThrows(InvalidBoardException.class, () -> gameEngine.validateBoard(board));
        }

        @Test
        void shouldRejectMoveIfGameIsAlreadyWonByPlayer() {
            Board board = createBoard(
                "X", "X", "X",
                "O", "O", "",
                "",  "",  ""
            );
            // Equal counts not even possible here, but test terminal check
            Board boardWithEquals = createBoard(
                "X", "X", "X",
                "O", "O", "O",
                "",  "",  ""
            );
            assertThrows(InvalidMoveException.class, () -> gameEngine.validateBoard(boardWithEquals));
        }

        @Test
        void shouldRejectMoveIfPositionIsOutside1To9() {
            Board board = new Board();
            assertThrows(InvalidMoveException.class, () -> gameEngine.validateMove(board, 0));
            assertThrows(InvalidMoveException.class, () -> gameEngine.validateMove(board, 10));
            assertThrows(InvalidMoveException.class, () -> gameEngine.validateMove(board, -5));
            assertThrows(InvalidMoveException.class, () -> gameEngine.validateMove(board, null));
        }

        @Test
        void shouldRejectMoveIfCellIsAlreadyOccupied() {
            Board board = createBoard(
                "X", "", "",
                "",  "", "",
                "",  "", ""
            );
            // Position 1 (index 0) occupied by X
            assertThrows(InvalidMoveException.class, () -> gameEngine.validateMove(board, 1));
        }

        @Test
        void shouldAllowValidMoveOnEmptyCell() {
            Board board = new Board();
            assertDoesNotThrow(() -> gameEngine.validateMove(board, 5));
        }
    }

    private static Board createBoard(String... cells) {
        return new Board(Arrays.asList(cells));
    }
}
