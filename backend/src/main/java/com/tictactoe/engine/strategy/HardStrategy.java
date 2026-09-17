package com.tictactoe.engine.strategy;

import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.springframework.stereotype.Component;

/**
 * HARD Strategy:
 * Placeholder for the unbeatable Minimax algorithm with alpha-beta pruning.
 * Intentionally deferred to MVP2 Phase 2.
 */
@Component
public class HardStrategy implements MoveStrategy {

    @Override
    public int chooseMove(Board board) {
        throw new UnsupportedOperationException(
            "Hard difficulty AI (Minimax with alpha-beta pruning) is not implemented in Phase 1 and will be introduced in MVP2 Phase 2."
        );
    }

    @Override
    public Difficulty getDifficulty() {
        return Difficulty.HARD;
    }
}
