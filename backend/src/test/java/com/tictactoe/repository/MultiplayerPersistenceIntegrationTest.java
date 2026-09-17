package com.tictactoe.repository;

import com.tictactoe.dto.CreateUserRequest;
import com.tictactoe.dto.GameHistoryResponse;
import com.tictactoe.dto.LeaderboardEntryResponse;
import com.tictactoe.dto.UserProfileResponse;
import com.tictactoe.entity.GameRecord;
import com.tictactoe.entity.ParticipantOutcome;
import com.tictactoe.model.GameMode;
import com.tictactoe.model.GameStatus;
import com.tictactoe.service.GameHistoryService;
import com.tictactoe.service.LeaderboardService;
import com.tictactoe.service.PlayerProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MultiplayerPersistenceIntegrationTest {

    @Autowired
    private PlayerProfileService playerProfileService;

    @Autowired
    private GameHistoryService gameHistoryService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Test
    @DisplayName("Completed multiplayer game persists, appears in user history, and updates leaderboard")
    void testMultiplayerGamePersistenceAndLeaderboard() {
        UserProfileResponse alice = playerProfileService.createProfile(new CreateUserRequest("MPAlice1"));
        UserProfileResponse bob = playerProfileService.createProfile(new CreateUserRequest("MPBob1"));

        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();

        // Alice (X) beats Bob (O)
        GameRecord record = gameHistoryService.recordCompletedMultiplayerGame(
                alice.getId(), bob.getId(), GameStatus.PLAYER_ONE_WON, start, end
        );

        assertNotNull(record.getId());
        assertEquals(GameMode.MULTIPLAYER, record.getGameMode());
        assertEquals(2, record.getParticipants().size());

        // Verify Alice's history
        Page<GameHistoryResponse> aliceHistory = gameHistoryService.getUserHistory(alice.getId(), PageRequest.of(0, 10));
        assertEquals(1, aliceHistory.getTotalElements());
        GameHistoryResponse aliceGame = aliceHistory.getContent().get(0);
        assertEquals(GameMode.MULTIPLAYER, aliceGame.getGameMode());
        assertEquals(ParticipantOutcome.WIN, aliceGame.getOutcome());
        assertEquals("MPBob1", aliceGame.getOpponent());

        // Verify Bob's history
        Page<GameHistoryResponse> bobHistory = gameHistoryService.getUserHistory(bob.getId(), PageRequest.of(0, 10));
        assertEquals(1, bobHistory.getTotalElements());
        GameHistoryResponse bobGame = bobHistory.getContent().get(0);
        assertEquals(GameMode.MULTIPLAYER, bobGame.getGameMode());
        assertEquals(ParticipantOutcome.LOSS, bobGame.getOutcome());
        assertEquals("MPAlice1", bobGame.getOpponent());

        // Verify Leaderboard stats
        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboard(10);
        LeaderboardEntryResponse aliceEntry = leaderboard.stream()
                .filter(e -> e.getUserId().equals(alice.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, aliceEntry.getWins());
        assertEquals(0, aliceEntry.getLosses());
        assertEquals(1, aliceEntry.getGamesPlayed());
        assertEquals(100.0, aliceEntry.getWinRate());

        LeaderboardEntryResponse bobEntry = leaderboard.stream()
                .filter(e -> e.getUserId().equals(bob.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, bobEntry.getWins());
        assertEquals(1, bobEntry.getLosses());
        assertEquals(1, bobEntry.getGamesPlayed());
        assertEquals(0.0, bobEntry.getWinRate());
    }

    @Test
    @DisplayName("Multiplayer draw game awards draws to both participants")
    void testMultiplayerDrawPersistence() {
        UserProfileResponse charlie = playerProfileService.createProfile(new CreateUserRequest("MPCharlie1"));
        UserProfileResponse dana = playerProfileService.createProfile(new CreateUserRequest("MPDana1"));

        gameHistoryService.recordCompletedMultiplayerGame(
                charlie.getId(), dana.getId(), GameStatus.DRAW, Instant.now(), Instant.now()
        );

        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboard(10);
        LeaderboardEntryResponse charlieEntry = leaderboard.stream()
                .filter(e -> e.getUserId().equals(charlie.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, charlieEntry.getDraws());
        assertEquals(0, charlieEntry.getWins());

        LeaderboardEntryResponse danaEntry = leaderboard.stream()
                .filter(e -> e.getUserId().equals(dana.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, danaEntry.getDraws());
        assertEquals(0, danaEntry.getWins());
    }
}
