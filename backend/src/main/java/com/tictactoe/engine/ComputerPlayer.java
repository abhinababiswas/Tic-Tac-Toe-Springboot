package com.tictactoe.engine;

import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.OptionalInt;
import java.util.Random;

/**
 * Computer decision-making engine for Tic-Tac-Toe.
 * Supports Easy (pure random) and Medium (win -> block -> random) strategies.
 */
@Component
public class ComputerPlayer {

    private final GameEngine gameEngine;
    private final Random random;

    @Autowired
    public ComputerPlayer(GameEngine gameEngine) {
        this(gameEngine, new Random());
    }

    public ComputerPlayer(GameEngine gameEngine, Random random) {
        this.gameEngine = gameEngine;
        this.random = random != null ? random : new Random();
    }

    /**
     * Chooses a move index (0-8) based on the specified difficulty level.
     *
     * @param board current board state
     * @param difficulty EASY or MEDIUM
     * @return 0-based cell index for computer's move
     */
    public int chooseMove(Board board, Difficulty difficulty) {
        if (difficulty == null) {
            difficulty = Difficulty.MEDIUM;
        }

        return switch (difficulty) {
            case EASY -> chooseEasyMove(board);
            case MEDIUM -> chooseMediumMove(board);
        };
    }

    /**
     * EASY Strategy:
     * Picks uniformly at random from all available empty cells.
     * Has no heuristics or strategic intent.
     */
    public int chooseEasyMove(Board board) {
        List<Integer> emptyIndices = board.getEmptyIndices();
        if (emptyIndices.isEmpty()) {
            throw new IllegalStateException("Cannot choose move: No empty cells available on the board.");
        }
        int randomIndex = random.nextInt(emptyIndices.size());
        return emptyIndices.get(randomIndex);
    }

    /**
     * MEDIUM Strategy:
     * 1. Priority 1 (Win): If computer can win immediately, take the winning cell.
     * 2. Priority 2 (Block): If player can win immediately, take that cell to block the player.
     * 3. Priority 3 (Random): Otherwise, pick a random available cell.
     */
    public int chooseMediumMove(Board board) {
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
        return chooseEasyMove(board);
    }
}
