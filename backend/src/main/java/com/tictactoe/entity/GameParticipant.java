package com.tictactoe.entity;

import com.tictactoe.model.PlayerSymbol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Join entity representing a participant in a specific game match.
 * Links a game to a player profile (or null for the computer player),
 * their assigned symbol ('X' or 'O'), participant type (HUMAN vs COMPUTER),
 * and individual game outcome (WIN, LOSS, DRAW).
 */
@Entity
@Table(name = "game_participants")
public class GameParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameRecord game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = true)
    private PlayerProfile player;

    @Enumerated(EnumType.STRING)
    @Column(name = "symbol", nullable = false, length = 5)
    private PlayerSymbol symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    private ParticipantType participantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private ParticipantOutcome outcome;

    public GameParticipant() {
    }

    public GameParticipant(GameRecord game, PlayerProfile player, PlayerSymbol symbol, ParticipantType participantType, ParticipantOutcome outcome) {
        this.game = game;
        this.player = player;
        this.symbol = symbol;
        this.participantType = participantType;
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GameRecord getGame() {
        return game;
    }

    public void setGame(GameRecord game) {
        this.game = game;
    }

    public PlayerProfile getPlayer() {
        return player;
    }

    public void setPlayer(PlayerProfile player) {
        this.player = player;
    }

    public PlayerSymbol getSymbol() {
        return symbol;
    }

    public void setSymbol(PlayerSymbol symbol) {
        this.symbol = symbol;
    }

    public ParticipantType getParticipantType() {
        return participantType;
    }

    public void setParticipantType(ParticipantType participantType) {
        this.participantType = participantType;
    }

    public ParticipantOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ParticipantOutcome outcome) {
        this.outcome = outcome;
    }
}
