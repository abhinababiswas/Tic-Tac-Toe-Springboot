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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GameRecordRepositoryTest {

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should persist game record with participants and retrieve paginated user history")
    void shouldPersistAndRetrieveUserHistory() {
        PlayerProfile alice = playerProfileRepository.save(new PlayerProfile("alice"));
        PlayerProfile bob = playerProfileRepository.save(new PlayerProfile("bob"));
        entityManager.flush();

        Instant now = Instant.now();

        // Game 1 for Alice (earlier)
        GameRecord game1 = new GameRecord(GameMode.COMPUTER, Difficulty.EASY, GameStatus.PLAYER_WON, GameResult.PLAYER_WIN,
            now.minus(10, ChronoUnit.MINUTES), now.minus(9, ChronoUnit.MINUTES));
        game1.addParticipant(new GameParticipant(game1, alice, PlayerSymbol.X, ParticipantType.HUMAN, ParticipantOutcome.WIN));
        game1.addParticipant(new GameParticipant(game1, null, PlayerSymbol.O, ParticipantType.COMPUTER, ParticipantOutcome.LOSS));
        gameRecordRepository.save(game1);

        // Game 2 for Alice (later)
        GameRecord game2 = new GameRecord(GameMode.COMPUTER, Difficulty.HARD, GameStatus.COMPUTER_WON, GameResult.COMPUTER_WIN,
            now.minus(5, ChronoUnit.MINUTES), now.minus(4, ChronoUnit.MINUTES));
        game2.addParticipant(new GameParticipant(game2, alice, PlayerSymbol.X, ParticipantType.HUMAN, ParticipantOutcome.LOSS));
        game2.addParticipant(new GameParticipant(game2, null, PlayerSymbol.O, ParticipantType.COMPUTER, ParticipantOutcome.WIN));
        gameRecordRepository.save(game2);

        // Game for Bob
        GameRecord gameBob = new GameRecord(GameMode.COMPUTER, Difficulty.MEDIUM, GameStatus.DRAW, GameResult.DRAW,
            now.minus(2, ChronoUnit.MINUTES), now.minus(1, ChronoUnit.MINUTES));
        gameBob.addParticipant(new GameParticipant(gameBob, bob, PlayerSymbol.X, ParticipantType.HUMAN, ParticipantOutcome.DRAW));
        gameBob.addParticipant(new GameParticipant(gameBob, null, PlayerSymbol.O, ParticipantType.COMPUTER, ParticipantOutcome.DRAW));
        gameRecordRepository.save(gameBob);

        entityManager.flush();

        // Query Alice's history with page size 1
        Page<GameRecord> page0 = gameRecordRepository.findByPlayerId(alice.getId(), PageRequest.of(0, 1));
        assertThat(page0.getTotalElements()).isEqualTo(2);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent()).hasSize(1);
        // Order by completedAt DESC, so game2 should be first
        assertThat(page0.getContent().get(0).getId()).isEqualTo(game2.getId());

        // Page 1 for Alice
        Page<GameRecord> page1 = gameRecordRepository.findByPlayerId(alice.getId(), PageRequest.of(1, 1));
        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).getId()).isEqualTo(game1.getId());

        // Verify Bob's games are isolated
        Page<GameRecord> bobGames = gameRecordRepository.findByPlayerId(bob.getId(), PageRequest.of(0, 10));
        assertThat(bobGames.getTotalElements()).isEqualTo(1);
        assertThat(bobGames.getContent().get(0).getId()).isEqualTo(gameBob.getId());
    }
}
