package com.tictactoe.engine;

import com.tictactoe.engine.strategy.EasyStrategy;
import com.tictactoe.engine.strategy.HardStrategy;
import com.tictactoe.engine.strategy.MediumStrategy;
import com.tictactoe.engine.strategy.MoveStrategy;
import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Computer decision-making coordinator for Tic-Tac-Toe.
 * Delegates move calculations to concrete MoveStrategy implementations
 * based on the requested Difficulty level.
 */
@Component
public class ComputerPlayer {

    private final Map<Difficulty, MoveStrategy> strategies = new EnumMap<>(Difficulty.class);

    @Autowired
    public ComputerPlayer(List<MoveStrategy> strategyList) {
        for (MoveStrategy strategy : strategyList) {
            strategies.put(strategy.getDifficulty(), strategy);
        }
    }

    public ComputerPlayer(GameEngine gameEngine) {
        this(gameEngine, new Random());
    }

    public ComputerPlayer(GameEngine gameEngine, Random random) {
        Random rng = random != null ? random : new Random();
        EasyStrategy easy = new EasyStrategy(rng);
        MediumStrategy medium = new MediumStrategy(gameEngine, easy);
        HardStrategy hard = new HardStrategy();

        strategies.put(Difficulty.EASY, easy);
        strategies.put(Difficulty.MEDIUM, medium);
        strategies.put(Difficulty.HARD, hard);
    }

    public ComputerPlayer(Map<Difficulty, MoveStrategy> strategyMap) {
        if (strategyMap != null) {
            strategies.putAll(strategyMap);
        }
    }

    /**
     * Chooses a move index (0-8) based on the specified difficulty level.
     *
     * @param board current board state
     * @param difficulty EASY, MEDIUM, or HARD
     * @return 0-based cell index for computer's move
     */
    public int chooseMove(Board board, Difficulty difficulty) {
        if (difficulty == null) {
            difficulty = Difficulty.MEDIUM;
        }

        MoveStrategy strategy = strategies.get(difficulty);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy available for difficulty: " + difficulty);
        }

        return strategy.chooseMove(board);
    }

    /**
     * Backward-compatible convenience method for Easy moves.
     */
    public int chooseEasyMove(Board board) {
        return chooseMove(board, Difficulty.EASY);
    }

    /**
     * Backward-compatible convenience method for Medium moves.
     */
    public int chooseMediumMove(Board board) {
        return chooseMove(board, Difficulty.MEDIUM);
    }
}
