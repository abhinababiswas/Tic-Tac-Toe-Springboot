package com.tictactoe.engine.strategy;

import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * EASY Strategy:
 * Selects a move uniformly at random from all available empty cells.
 * Has no heuristics or strategic intent.
 */
@Component
public class EasyStrategy implements MoveStrategy {

    private final Random random;

    @Autowired
    public EasyStrategy() {
        this(new Random());
    }

    public EasyStrategy(Random random) {
        this.random = random != null ? random : new Random();
    }

    @Override
    public int chooseMove(Board board) {
        List<Integer> emptyIndices = board.getEmptyIndices();
        if (emptyIndices.isEmpty()) {
            throw new IllegalStateException("Cannot choose move: No empty cells available on the board.");
        }
        int randomIndex = random.nextInt(emptyIndices.size());
        return emptyIndices.get(randomIndex);
    }

    @Override
    public Difficulty getDifficulty() {
        return Difficulty.EASY;
    }
}
