package com.tictactoe.engine.strategy;

import com.tictactoe.engine.GameEngine;
import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.OptionalInt;
import java.util.Random;

/**
 * MEDIUM Strategy:
 * 1. Priority 1 (Win): If computer can win immediately, take the winning cell.
 * 2. Priority 2 (Block): If player can win immediately, take that cell to block the player.
 * 3. Priority 3 (Random): Otherwise, pick a random available cell.
 */
@Component
public class MediumStrategy implements MoveStrategy {

    private final GameEngine gameEngine;
    private final EasyStrategy easyStrategy;

    @Autowired
    public MediumStrategy(GameEngine gameEngine, EasyStrategy easyStrategy) {
        this.gameEngine = gameEngine;
        this.easyStrategy = easyStrategy;
    }

    public MediumStrategy(GameEngine gameEngine, Random random) {
        this.gameEngine = gameEngine;
        this.easyStrategy = new EasyStrategy(random);
    }

    @Override
    public int chooseMove(Board board) {
        // Priority 1: Check if computer ('O') can win immediately
        OptionalInt winningMove = gameEngine.findWinningMoveIndex(board, Board.COMPUTER_SYMBOL);
        if (winningMove.isPresent()) {
            return winningMove.getAsInt();
        }

        // Priority 2: Check if player ('X') threatens an immediate win, and block it
        OptionalInt blockingMove = gameEngine.findWinningMoveIndex(board, Board.PLAYER_SYMBOL);
        if (blockingMove.isPresent()) {
            return blockingMove.getAsInt();
        }

        // Priority 3: Fallback to random empty position
        return easyStrategy.chooseMove(board);
    }

    @Override
    public Difficulty getDifficulty() {
        return Difficulty.MEDIUM;
    }
}
