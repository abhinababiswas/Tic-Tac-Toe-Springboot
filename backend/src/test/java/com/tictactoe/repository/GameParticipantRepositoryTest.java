package com.tictactoe.repository;

import com.tictactoe.entity.GameParticipant;
import com.tictactoe.entity.GameRecord;
import com.tictactoe.entity.GameResult;
import com.tictactoe.entity.ParticipantOutcome;
import com.tictactoe.entity.ParticipantType;
import com.tictactoe.entity.PlayerProfile;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.GameMode;
import com.tictactoe.model.GameStatus;
import com.tictactoe.model.PlayerSymbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GameParticipantRepositoryTest {

    @Autowired
    private GameParticipantRepository gameParticipantRepository;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should correctly aggregate stats for User A (5W, 2L, 1D), User B (3W, 1L, 2D), and User C (0 games)")
    void shouldCorrectlyAggregateStats() {
        PlayerProfile userA = playerProfileRepository.save(new PlayerProfile("UserA"));
        PlayerProfile userB = playerProfileRepository.save(new PlayerProfile("UserB"));
        PlayerProfile userC = playerProfileRepository.save(new PlayerProfile("UserC"));
        entityManager.flush();

        // User A: 5 wins, 2 losses, 1 draw
        createGames(userA, 5, ParticipantOutcome.WIN, GameStatus.PLAYER_WON, GameResult.PLAYER_WIN);
        createGames(userA, 2, ParticipantOutcome.LOSS, GameStatus.COMPUTER_WON, GameResult.COMPUTER_WIN);
        createGames(userA, 1, ParticipantOutcome.DRAW, GameStatus.DRAW, GameResult.DRAW);

        // User B: 3 wins, 1 loss, 2 draws
        createGames(userB, 3, ParticipantOutcome.WIN, GameStatus.PLAYER_WON, GameResult.PLAYER_WIN);
        createGames(userB, 1, ParticipantOutcome.LOSS, GameStatus.COMPUTER_WON, GameResult.COMPUTER_WIN);
        createGames(userB, 2, ParticipantOutcome.DRAW, GameStatus.DRAW, GameResult.DRAW);

        // User C: 0 games

        entityManager.flush();

        List<PlayerStatProjection> stats = gameParticipantRepository.aggregatePlayerStats();
        assertThat(stats).hasSize(3);

        PlayerStatProjection statA = stats.stream()
            .filter(s -> s.getPlayerId().equals(userA.getId()))
            .findFirst().orElseThrow();
        assertThat(statA.getGamesPlayed()).isEqualTo(8);
        assertThat(statA.getWins()).isEqualTo(5);
        assertThat(statA.getLosses()).isEqualTo(2);
        assertThat(statA.getDraws()).isEqualTo(1);

        PlayerStatProjection statB = stats.stream()
            .filter(s -> s.getPlayerId().equals(userB.getId()))
            .findFirst().orElseThrow();
        assertThat(statB.getGamesPlayed()).isEqualTo(6);
        assertThat(statB.getWins()).isEqualTo(3);
        assertThat(statB.getLosses()).isEqualTo(1);
        assertThat(statB.getDraws()).isEqualTo(2);

        PlayerStatProjection statC = stats.stream()
            .filter(s -> s.getPlayerId().equals(userC.getId()))
            .findFirst().orElseThrow();
        assertThat(statC.getGamesPlayed()).isEqualTo(0);
        assertThat(statC.getWins()).isEqualTo(0);
        assertThat(statC.getLosses()).isEqualTo(0);
        assertThat(statC.getDraws()).isEqualTo(0);
    }

    private void createGames(PlayerProfile player, int count, ParticipantOutcome outcome, GameStatus status, GameResult result) {
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            GameRecord game = new GameRecord(GameMode.COMPUTER, Difficulty.MEDIUM, status, result, now, now);
            game.addParticipant(new GameParticipant(game, player, PlayerSymbol.X, ParticipantType.HUMAN, outcome));
            game.addParticipant(new GameParticipant(game, null, PlayerSymbol.O, ParticipantType.COMPUTER,
                outcome == ParticipantOutcome.WIN ? ParticipantOutcome.LOSS : (outcome == ParticipantOutcome.LOSS ? ParticipantOutcome.WIN : ParticipantOutcome.DRAW)));
            gameRecordRepository.save(game);
        }
    }
}
