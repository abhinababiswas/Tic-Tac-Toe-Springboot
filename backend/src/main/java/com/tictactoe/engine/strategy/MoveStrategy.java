package com.tictactoe.engine.strategy;

import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;

/**
 * Strategy interface for calculating computer player moves.
 * Enables clean separation of AI algorithms (random, rule-based heuristics, Minimax)
 * without coupling the game orchestration or controller layers.
 */
public interface MoveStrategy {

    /**
     * Calculates the next move index (0-8) on the given board.
     *
     * @param board current board state
     * @return 0-based cell index for the chosen move
     */
    int chooseMove(Board board);

    /**
     * Returns the difficulty level supported by this strategy.
     *
     * @return Difficulty enum value
     */
    Difficulty getDifficulty();
}
