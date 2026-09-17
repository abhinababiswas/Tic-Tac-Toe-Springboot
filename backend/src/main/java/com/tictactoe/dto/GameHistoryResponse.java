package com.tictactoe.dto;

import com.tictactoe.entity.GameResult;
import com.tictactoe.entity.ParticipantOutcome;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.GameMode;
import com.tictactoe.model.GameStatus;

import java.time.Instant;

/**
 * DTO representing an individual game in a player's history.
 */
public class GameHistoryResponse {

    private Long gameId;
    private GameMode gameMode;
    private Difficulty difficulty;
    private GameStatus status;
    private GameResult result;
    private ParticipantOutcome outcome;
    private String opponent;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationSeconds;

    public GameHistoryResponse() {
    }

    public GameHistoryResponse(Long gameId, GameMode gameMode, Difficulty difficulty, GameStatus status,
                               GameResult result, ParticipantOutcome outcome, String opponent,
                               Instant startedAt, Instant completedAt, Long durationSeconds) {
        this.gameId = gameId;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.status = status;
        this.result = result;
        this.outcome = outcome;
        this.opponent = opponent;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.durationSeconds = durationSeconds;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public GameResult getResult() {
        return result;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    public ParticipantOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ParticipantOutcome outcome) {
        this.outcome = outcome;
    }

    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
