package com.tictactoe.engine.strategy;

import com.tictactoe.engine.GameEngine;
import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HARD Strategy:
 * Unbeatable Tic-Tac-Toe opponent using the Minimax algorithm with alpha-beta pruning.
 * 
 * Algorithm Characteristics:
 * - Computer is the Maximizing Player (symbol 'O')
 * - Human Player is the Minimizing Player (symbol 'X')
 * - Depth-sensitive evaluation scoring:
 *     - Computer Win:  +10 - depth (favors fastest possible victory)
 *     - Player Win:    depth - 10 (favors prolonging game when defeat is unavoidable)
 *     - Draw:          0
 * - Alpha-Beta Pruning:
 *     - alpha: best explored value for maximizer (initial -infinity)
 *     - beta:  best explored value for minimizer (initial +infinity)
 *     - prunes remaining subtrees when beta <= alpha
 * - Deterministic tie-breaking:
 *     - Evaluates candidate moves in natural index order (0 through 8)
 *     - Selects the first move achieving the highest score
 * - State immutability:
 *     - Explores hypothetical branches via immutable Board.withMove(...)
 *     - Original board is never mutated
 */
@Component
public class HardStrategy implements MoveStrategy {

    private final GameEngine gameEngine;

    @Autowired
    public HardStrategy(GameEngine gameEngine) {
        this.gameEngine = gameEngine != null ? gameEngine : new GameEngine();
    }

    public HardStrategy() {
        this(new GameEngine());
    }

    @Override
    public int chooseMove(Board board) {
        return chooseMoveInternal(board, true, null);
    }

    /**
     * Test-accessible entry point enabling measurement of alpha-beta pruning node reductions.
     */
    public int chooseMoveWithMetrics(Board board, boolean enablePruning, Metrics metrics) {
        return chooseMoveInternal(board, enablePruning, metrics);
    }

    private int chooseMoveInternal(Board board, boolean enablePruning, Metrics metrics) {
        List<Integer> emptyIndices = board.getEmptyIndices();
        if (emptyIndices.isEmpty()) {
            throw new IllegalStateException("Cannot choose move: No empty cells available on the board.");
        }

        int bestMove = -1;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (int index : emptyIndices) {
            Board simulated = board.withMove(index, Board.COMPUTER_SYMBOL);
            if (metrics != null) {
                metrics.incrementEvaluations();
            }

            // The next turn is the human minimizing player ('X') at depth 1
            int score = minimax(simulated, 1, false, alpha, beta, enablePruning, metrics);

            if (score > bestScore) {
                bestScore = score;
                bestMove = index;
            }

            alpha = Math.max(alpha, bestScore);
            if (enablePruning && beta <= alpha) {
                break;
            }
        }

        return bestMove;
    }

    /**
     * Recursive Minimax evaluation with alpha-beta pruning.
     */
    private int minimax(Board board, int depth, boolean isMaximizing, int alpha, int beta, boolean enablePruning, Metrics metrics) {
        if (metrics != null) {
            metrics.incrementEvaluations();
        }

        // Terminal State 1: Computer Win (+10 minus depth)
        if (gameEngine.checkWinner(board, Board.COMPUTER_SYMBOL)) {
            return 10 - depth;
        }

        // Terminal State 2: Human Player Win (depth minus 10)
        if (gameEngine.checkWinner(board, Board.PLAYER_SYMBOL)) {
            return depth - 10;
        }

        // Terminal State 3: Draw (Board full with no winner)
        if (board.isFull()) {
            return 0;
        }

        List<Integer> emptyIndices = board.getEmptyIndices();

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int index : emptyIndices) {
                Board simulated = board.withMove(index, Board.COMPUTER_SYMBOL);
                int eval = minimax(simulated, depth + 1, false, alpha, beta, enablePruning, metrics);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (enablePruning && beta <= alpha) {
                    break; // Beta cutoff
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int index : emptyIndices) {
                Board simulated = board.withMove(index, Board.PLAYER_SYMBOL);
                int eval = minimax(simulated, depth + 1, true, alpha, beta, enablePruning, metrics);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (enablePruning && beta <= alpha) {
                    break; // Alpha cutoff
                }
            }
            return minEval;
        }
    }

    @Override
    public Difficulty getDifficulty() {
        return Difficulty.HARD;
    }

    /**
     * Lightweight metrics counter for testing and verifying alpha-beta pruning efficiency.
     */
    public static class Metrics {
        private int evaluations = 0;

        public void incrementEvaluations() {
            evaluations++;
        }

        public int getEvaluations() {
            return evaluations;
        }
    }
}
