package com.tictactoe.service;

import com.tictactoe.dto.LeaderboardEntryResponse;
import com.tictactoe.repository.GameParticipantRepository;
import com.tictactoe.repository.PlayerStatProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private GameParticipantRepository gameParticipantRepository;

    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = new LeaderboardService(gameParticipantRepository);
    }

    @Test
    @DisplayName("Should rank users by wins DESC, win rate DESC, games played DESC and compute win rates safely")
    void shouldRankUsersAndCalculateWinRates() {
        PlayerStatProjection statA = createStat(1L, "UserA", 8L, 5L, 2L, 1L);
        PlayerStatProjection statB = createStat(2L, "UserB", 6L, 3L, 1L, 2L);
        PlayerStatProjection statC = createStat(3L, "UserC", 0L, 0L, 0L, 0L);

        when(gameParticipantRepository.aggregatePlayerStats()).thenReturn(List.of(statB, statC, statA));

        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboard(20);

        assertThat(leaderboard).hasSize(3);

        // Rank 1: UserA (5 wins, 62.5% win rate)
        LeaderboardEntryResponse rank1 = leaderboard.get(0);
        assertThat(rank1.getRank()).isEqualTo(1);
        assertThat(rank1.getUsername()).isEqualTo("UserA");
        assertThat(rank1.getWins()).isEqualTo(5);
        assertThat(rank1.getLosses()).isEqualTo(2);
        assertThat(rank1.getDraws()).isEqualTo(1);
        assertThat(rank1.getGamesPlayed()).isEqualTo(8);
        assertThat(rank1.getWinRate()).isEqualTo(62.5);

        // Rank 2: UserB (3 wins, 50.0% win rate)
        LeaderboardEntryResponse rank2 = leaderboard.get(1);
        assertThat(rank2.getRank()).isEqualTo(2);
        assertThat(rank2.getUsername()).isEqualTo("UserB");
        assertThat(rank2.getWins()).isEqualTo(3);
        assertThat(rank2.getLosses()).isEqualTo(1);
        assertThat(rank2.getDraws()).isEqualTo(2);
        assertThat(rank2.getGamesPlayed()).isEqualTo(6);
        assertThat(rank2.getWinRate()).isEqualTo(50.0);

        // Rank 3: UserC (0 games, 0.0% win rate - safe zero division)
        LeaderboardEntryResponse rank3 = leaderboard.get(2);
        assertThat(rank3.getRank()).isEqualTo(3);
        assertThat(rank3.getUsername()).isEqualTo("UserC");
        assertThat(rank3.getWins()).isEqualTo(0);
        assertThat(rank3.getGamesPlayed()).isEqualTo(0);
        assertThat(rank3.getWinRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should respect requested result limit")
    void shouldRespectLimit() {
        PlayerStatProjection statA = createStat(1L, "UserA", 10L, 8L, 2L, 0L);
        PlayerStatProjection statB = createStat(2L, "UserB", 10L, 7L, 3L, 0L);
        PlayerStatProjection statC = createStat(3L, "UserC", 10L, 6L, 4L, 0L);

        when(gameParticipantRepository.aggregatePlayerStats()).thenReturn(List.of(statA, statB, statC));

        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboard(2);
        assertThat(leaderboard).hasSize(2);
        assertThat(leaderboard.get(0).getUsername()).isEqualTo("UserA");
        assertThat(leaderboard.get(1).getUsername()).isEqualTo("UserB");
    }

    private PlayerStatProjection createStat(Long id, String username, Long played, Long wins, Long losses, Long draws) {
        return new PlayerStatProjection() {
            @Override
            public Long getPlayerId() { return id; }

            @Override
            public String getUsername() { return username; }

            @Override
            public Long getGamesPlayed() { return played; }

            @Override
            public Long getWins() { return wins; }

            @Override
            public Long getLosses() { return losses; }

            @Override
            public Long getDraws() { return draws; }
        };
    }
}
