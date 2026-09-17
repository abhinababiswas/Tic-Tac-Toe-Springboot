package com.tictactoe.entity;

import com.tictactoe.model.Difficulty;
import com.tictactoe.model.GameMode;
import com.tictactoe.model.GameStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent representation of a game match.
 * Server-authoritative record of players, rules, final status, result, and timestamps.
 */
@Entity
@Table(name = "game_records")
public class GameRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false, length = 20)
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private GameResult result;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameParticipant> participants = new ArrayList<>();

    public GameRecord() {
    }

    public GameRecord(GameMode gameMode, Difficulty difficulty, GameStatus status, GameResult result, Instant startedAt, Instant completedAt) {
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.status = status;
        this.result = result;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.startedAt == null) {
            this.startedAt = this.createdAt;
        }
        if (this.completedAt == null) {
            this.completedAt = Instant.now();
        }
    }

    public void addParticipant(GameParticipant participant) {
        participants.add(participant);
        participant.setGame(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<GameParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<GameParticipant> participants) {
        this.participants = participants;
    }
}
