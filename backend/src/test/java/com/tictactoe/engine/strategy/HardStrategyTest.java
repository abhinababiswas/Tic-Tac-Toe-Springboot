package com.tictactoe.engine.strategy;

import com.tictactoe.engine.GameEngine;
import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class HardStrategyTest {

    private GameEngine gameEngine;
    private HardStrategy hardStrategy;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
        hardStrategy = new HardStrategy(gameEngine);
    }

    @Test
    @DisplayName("Returns Difficulty.HARD")
    void shouldReturnHardDifficulty() {
        assertEquals(Difficulty.HARD, hardStrategy.getDifficulty());
    }

    @Test
    @DisplayName("Test 1: Immediate Win - Computer takes winning move immediately")
    void shouldTakeImmediateWinningMove() {
        // O | O | - (position 3 / index 2 wins for O)
        // X | X | -
        // - | - | -
        Board board = createBoard(
            "O", "O", "",
            "X", "X", "",
            "",  "",  ""
        );
        int move = hardStrategy.chooseMove(board);
        assertEquals(2, move, "Hard AI must select winning move at index 2.");
    }

    @Test
    @DisplayName("Test 2: Must Block - Computer blocks immediate human winning threat")
    void shouldBlockImmediatePlayerWin() {
        // X | X | - (position 3 / index 2 threatens win for X)
        // O | - | -
        // - | - | O
        Board board = createBoard(
            "X", "X", "",
            "O", "",  "",
            "",  "",  "O"
        );
        int move = hardStrategy.chooseMove(board);
        assertEquals(2, move, "Hard AI must block human win at index 2.");
    }

    @Test
    @DisplayName("Test 3: Fork Prevention - Blocks opposite corner fork")
    void shouldPreventCornerFork() {
        // X | - | -
        // - | O | -
        // - | - | X
        // If human plays opposite corners and computer plays center,
        // computer must play an EDGE (1, 3, 5, 7) to force human to defend, preventing fork.
        // Playing a corner (2, 6) allows human to create a double threat (fork) on the next move.
        Board board = createBoard(
            "X", "",  "",
            "",  "O", "",
            "",  "",  "X"
        );
        int move = hardStrategy.chooseMove(board);
        List<Integer> edgeIndices = Arrays.asList(1, 3, 5, 7);
        assertTrue(edgeIndices.contains(move), "Computer must play an edge to prevent corner fork, but played: " + move);
    }

    @Test
    @DisplayName("Test 4: Forced Draw - Perfect defense against center opening")
    void shouldDefendAgainstCenterOpening() {
        // - | - | -
        // - | X | -
        // - | - | -
        // If human plays center, computer's optimal response is any corner (0, 2, 6, 8).
        Board board = createBoard(
            "", "", "",
            "", "X", "",
            "", "", ""
        );
        int move = hardStrategy.chooseMove(board);
        List<Integer> cornerIndices = Arrays.asList(0, 2, 6, 8);
        assertTrue(cornerIndices.contains(move), "Against center opening, computer must take a corner, but played: " + move);
    }

    @Test
    @DisplayName("Test 5: Forced Computer Win - Exploits human blunder to force win")
    void shouldExploitBlunderToForceWin() {
        // O | X | X
        // - | O | -
        // - | - | -
        // Computer (O) has center and top-left; human blundered.
        // Computer can force win along diagonal (index 8).
        Board board = createBoard(
            "O", "X", "X",
            "",  "O", "",
            "",  "",  ""
        );
        int move = hardStrategy.chooseMove(board);
        assertEquals(8, move, "Computer must take index 8 to complete diagonal win.");
    }

    @Test
    @DisplayName("Test 6: Full Board - Throws IllegalStateException when board is full")
    void shouldThrowWhenBoardIsFull() {
        Board fullBoard = createBoard(
            "X", "O", "X",
            "X", "O", "O",
            "O", "X", "X"
        );
        assertThrows(IllegalStateException.class, () -> hardStrategy.chooseMove(fullBoard));
    }

    @Test
    @DisplayName("Test 7: Single Legal Move - Selects the only remaining open cell")
    void shouldPickSoleRemainingCell() {
        Board board = createBoard(
            "X", "O", "X",
            "O", "X", "O",
            "O", "X", ""
        );
        int move = hardStrategy.chooseMove(board);
        assertEquals(8, move, "Computer must pick the only available position (index 8).");
    }

    @Test
    @DisplayName("Test 8: Board Integrity - Does not mutate original board instance")
    void shouldNotMutateOriginalBoard() {
        Board board = createBoard(
            "X", "", "",
            "",  "O", "",
            "",  "",  ""
        );
        List<String> originalCells = List.copyOf(board.getCells());

        hardStrategy.chooseMove(board);

        assertEquals(originalCells, board.getCells(), "Original board must remain completely unchanged after Minimax.");
    }

    @Test
    @DisplayName("Test 9: Deterministic Tie-Breaking - Consistent choice on symmetric boards")
    void shouldBreakTiesDeterministically() {
        // Symmetric board after player plays center
        Board board = createBoard(
            "", "", "",
            "", "X", "",
            "", "", ""
        );
        int firstChoice = hardStrategy.chooseMove(board);
        int secondChoice = hardStrategy.chooseMove(board);
        int thirdChoice = hardStrategy.chooseMove(board);

        assertEquals(firstChoice, secondChoice);
        assertEquals(secondChoice, thirdChoice);
        assertEquals(0, firstChoice, "First optimal corner in natural order (0..8) should be chosen.");
    }

    @Test
    @DisplayName("Test 10: Alpha-Beta Pruning Verification - Strictly reduces evaluated node count")
    void shouldDemonstrateAlphaBetaPruningReduction() {
        // Near-empty board (player plays index 0)
        Board board = createBoard(
            "X", "", "",
            "",  "", "",
            "",  "", ""
        );

        HardStrategy.Metrics metricsWithPruning = new HardStrategy.Metrics();
        hardStrategy.chooseMoveWithMetrics(board, true, metricsWithPruning);

        HardStrategy.Metrics metricsWithoutPruning = new HardStrategy.Metrics();
        hardStrategy.chooseMoveWithMetrics(board, false, metricsWithoutPruning);

        int countPruned = metricsWithPruning.getEvaluations();
        int countUnpruned = metricsWithoutPruning.getEvaluations();

        assertTrue(countPruned > 0, "Pruned evaluations must be positive.");
        assertTrue(countUnpruned > countPruned,
            "Alpha-beta pruning must evaluate strictly fewer nodes than full minimax. Pruned: " +
            countPruned + ", Unpruned: " + countUnpruned);
    }

    @Test
    @DisplayName("Test 11: Complete Game Simulations - Hard AI never loses across random opponents")
    void shouldNeverLoseAcrossMonteCarloSimulations() {
        Random random = new Random(12345);

        for (int game = 0; game < 100; game++) {
            Board board = new Board();

            // Game loop: Human ('X') plays random legal move; Computer ('O') plays HardStrategy
            while (!board.isFull() && gameEngine.getWinner(board).isEmpty()) {
                // Human turn
                List<Integer> humanEmpty = board.getEmptyIndices();
                int humanMove = humanEmpty.get(random.nextInt(humanEmpty.size()));
                board = board.withMove(humanMove, Board.PLAYER_SYMBOL);

                if (gameEngine.checkWinner(board, Board.PLAYER_SYMBOL)) {
                    fail("Human won in simulation game " + game + "! Hard AI must never lose.");
                }

                if (board.isFull() || gameEngine.checkWinner(board, Board.PLAYER_SYMBOL)) {
                    break;
                }

                // Computer turn
                int computerMove = hardStrategy.chooseMove(board);
                board = board.withMove(computerMove, Board.COMPUTER_SYMBOL);

                if (gameEngine.checkWinner(board, Board.PLAYER_SYMBOL)) {
                    fail("Human won in simulation game " + game + "! Hard AI must never lose.");
                }
            }

            // Assert outcome is either COMPUTER_WON or DRAW (Never PLAYER_WON)
            assertNotEquals(Board.PLAYER_SYMBOL, gameEngine.getWinner(board).orElse(null),
                "Hard AI lost game " + game);
        }
    }

    @Test
    @DisplayName("Test 12: Exhaustive First-Move Exploration - Hard AI never loses for all 9 human opening moves")
    void shouldNeverLoseForAllOpeningMoves() {
        // Player plays any of the 9 positions as their opening move
        for (int openingMove = 0; openingMove < Board.BOARD_SIZE; openingMove++) {
            Board board = new Board().withMove(openingMove, Board.PLAYER_SYMBOL);

            // Computer responds with Hard AI
            int compMove = hardStrategy.chooseMove(board);
            board = board.withMove(compMove, Board.COMPUTER_SYMBOL);

            assertFalse(gameEngine.checkWinner(board, Board.PLAYER_SYMBOL),
                "Human should not win on turn 1");
        }
    }

    private static Board createBoard(String... cells) {
        return new Board(Arrays.asList(cells));
    }
}
