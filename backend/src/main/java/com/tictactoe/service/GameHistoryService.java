package com.tictactoe.service;

import com.tictactoe.dto.GameHistoryResponse;
import com.tictactoe.entity.GameParticipant;
import com.tictactoe.entity.GameRecord;
import com.tictactoe.entity.GameResult;
import com.tictactoe.entity.ParticipantOutcome;
import com.tictactoe.entity.ParticipantType;
import com.tictactoe.entity.PlayerProfile;
import com.tictactoe.exception.ResourceNotFoundException;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.GameMode;
import com.tictactoe.model.GameStatus;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.repository.GameRecordRepository;
import com.tictactoe.repository.PlayerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Service managing persistence of completed game records and retrieval of player history.
 */
@Service
public class GameHistoryService {

    private final GameRecordRepository gameRecordRepository;
    private final PlayerProfileRepository playerProfileRepository;

    @Autowired
    public GameHistoryService(GameRecordRepository gameRecordRepository, PlayerProfileRepository playerProfileRepository) {
        this.gameRecordRepository = gameRecordRepository;
        this.playerProfileRepository = playerProfileRepository;
    }

    /**
     * Persists a server-authoritative record of a completed Player vs Computer match.
     */
    @Transactional
    public GameRecord recordCompletedComputerGame(Long userId, Difficulty difficulty, GameStatus terminalStatus,
                                                 Instant startedAt, Instant completedAt) {
        if (userId == null) {
            return null; // Guest game; do not persist
        }

        PlayerProfile player = playerProfileRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Cannot record game: User not found with id " + userId));

        if (!terminalStatus.isTerminal()) {
            throw new IllegalArgumentException("Cannot record incomplete game with status: " + terminalStatus);
        }

        Instant start = (startedAt != null) ? startedAt : Instant.now();
        Instant end = (completedAt != null) ? completedAt : Instant.now();

        GameResult gameResult;
        ParticipantOutcome humanOutcome;
        ParticipantOutcome computerOutcome;

        switch (terminalStatus) {
            case PLAYER_WON -> {
                gameResult = GameResult.PLAYER_WIN;
                humanOutcome = ParticipantOutcome.WIN;
                computerOutcome = ParticipantOutcome.LOSS;
            }
            case COMPUTER_WON -> {
                gameResult = GameResult.COMPUTER_WIN;
                humanOutcome = ParticipantOutcome.LOSS;
                computerOutcome = ParticipantOutcome.WIN;
            }
            case DRAW -> {
                gameResult = GameResult.DRAW;
                humanOutcome = ParticipantOutcome.DRAW;
                computerOutcome = ParticipantOutcome.DRAW;
            }
            default -> throw new IllegalArgumentException("Unsupported terminal status for single player: " + terminalStatus);
        }

        GameRecord gameRecord = new GameRecord(
            GameMode.COMPUTER,
            difficulty != null ? difficulty : Difficulty.MEDIUM,
            terminalStatus,
            gameResult,
            start,
            end
        );

        GameParticipant humanParticipant = new GameParticipant(
            gameRecord,
            player,
            PlayerSymbol.X,
            ParticipantType.HUMAN,
            humanOutcome
        );

        GameParticipant computerParticipant = new GameParticipant(
            gameRecord,
            null, // No DB user record for the computer
            PlayerSymbol.O,
            ParticipantType.COMPUTER,
            computerOutcome
        );

        gameRecord.addParticipant(humanParticipant);
        gameRecord.addParticipant(computerParticipant);

        return gameRecordRepository.save(gameRecord);
    }

    /**
     * Persists a server-authoritative record of a completed real-time multiplayer match.
     */
    @Transactional
    public GameRecord recordCompletedMultiplayerGame(Long playerXId, Long playerOId, GameStatus terminalStatus,
                                                     Instant startedAt, Instant completedAt) {
        if (terminalStatus == null || !terminalStatus.isTerminal()) {
            throw new IllegalArgumentException("Cannot record incomplete game with status: " + terminalStatus);
        }

        PlayerProfile playerX = (playerXId != null)
            ? playerProfileRepository.findById(playerXId).orElse(null)
            : null;
        PlayerProfile playerO = (playerOId != null)
            ? playerProfileRepository.findById(playerOId).orElse(null)
            : null;

        Instant start = (startedAt != null) ? startedAt : Instant.now();
        Instant end = (completedAt != null) ? completedAt : Instant.now();

        GameResult gameResult;
        ParticipantOutcome xOutcome;
        ParticipantOutcome oOutcome;

        switch (terminalStatus) {
            case PLAYER_ONE_WON -> {
                gameResult = GameResult.PLAYER_ONE_WIN;
                xOutcome = ParticipantOutcome.WIN;
                oOutcome = ParticipantOutcome.LOSS;
            }
            case PLAYER_TWO_WON -> {
                gameResult = GameResult.PLAYER_TWO_WIN;
                xOutcome = ParticipantOutcome.LOSS;
                oOutcome = ParticipantOutcome.WIN;
            }
            case DRAW -> {
                gameResult = GameResult.DRAW;
                xOutcome = ParticipantOutcome.DRAW;
                oOutcome = ParticipantOutcome.DRAW;
            }
            default -> throw new IllegalArgumentException("Unsupported multiplayer terminal status: " + terminalStatus);
        }

        GameRecord gameRecord = new GameRecord(
            GameMode.MULTIPLAYER,
            null, // No AI difficulty for PvP multiplayer
            terminalStatus,
            gameResult,
            start,
            end
        );

        GameParticipant participantX = new GameParticipant(
            gameRecord,
            playerX,
            PlayerSymbol.X,
            ParticipantType.HUMAN,
            xOutcome
        );

        GameParticipant participantO = new GameParticipant(
            gameRecord,
            playerO,
            PlayerSymbol.O,
            ParticipantType.HUMAN,
            oOutcome
        );

        gameRecord.addParticipant(participantX);
        gameRecord.addParticipant(participantO);

        return gameRecordRepository.save(gameRecord);
    }

    /**
     * Retrieves paginated game history for a specific registered user.
     */
    @Transactional(readOnly = true)
    public Page<GameHistoryResponse> getUserHistory(Long userId, Pageable pageable) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null.");
        }

        if (!playerProfileRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<GameRecord> records = gameRecordRepository.findByPlayerId(userId, pageable);

        return records.map(record -> {
            ParticipantOutcome outcome = null;
            String opponent = "Computer";

            for (GameParticipant p : record.getParticipants()) {
                if (p.getPlayer() != null && p.getPlayer().getId().equals(userId)) {
                    outcome = p.getOutcome();
                } else if (p.getParticipantType() == ParticipantType.HUMAN && p.getPlayer() != null) {
                    opponent = p.getPlayer().getUsername();
                }
            }

            long duration = 0;
            if (record.getStartedAt() != null && record.getCompletedAt() != null) {
                duration = Math.max(0, Duration.between(record.getStartedAt(), record.getCompletedAt()).toSeconds());
            }

            return new GameHistoryResponse(
                record.getId(),
                record.getGameMode(),
                record.getDifficulty(),
                record.getStatus(),
                record.getResult(),
                outcome,
                opponent,
                record.getStartedAt(),
                record.getCompletedAt(),
                duration
            );
        });
    }
}
