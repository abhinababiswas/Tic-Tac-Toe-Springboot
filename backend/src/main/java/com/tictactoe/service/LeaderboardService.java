package com.tictactoe.service;

import com.tictactoe.dto.LeaderboardEntryResponse;
import com.tictactoe.repository.GameParticipantRepository;
import com.tictactoe.repository.PlayerStatProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service calculating competitive leaderboard statistics from persistent game history.
 * Aggregates only completed games associated with registered user accounts.
 */
@Service
public class LeaderboardService {

    private final GameParticipantRepository gameParticipantRepository;

    @Autowired
    public LeaderboardService(GameParticipantRepository gameParticipantRepository) {
        this.gameParticipantRepository = gameParticipantRepository;
    }

    /**
     * Calculates the leaderboard ranking for registered players.
     *
     * @param limit maximum number of players to return
     * @return ranked list of leaderboard entries
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(int limit) {
        int maxResults = (limit > 0) ? Math.min(limit, 100) : 20;

        List<PlayerStatProjection> rawStats = gameParticipantRepository.aggregatePlayerStats();

        List<LeaderboardEntryResponse> entries = new ArrayList<>();

        for (PlayerStatProjection stat : rawStats) {
            long gamesPlayed = stat.getGamesPlayed() != null ? stat.getGamesPlayed() : 0L;
            long wins = stat.getWins() != null ? stat.getWins() : 0L;
            long losses = stat.getLosses() != null ? stat.getLosses() : 0L;
            long draws = stat.getDraws() != null ? stat.getDraws() : 0L;

            double winRate = 0.0;
            if (gamesPlayed > 0) {
                double rawRate = (wins * 100.0) / gamesPlayed;
                winRate = BigDecimal.valueOf(rawRate)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
            }

            entries.add(new LeaderboardEntryResponse(
                0, // rank will be assigned after sorting
                stat.getPlayerId(),
                stat.getUsername(),
                gamesPlayed,
                wins,
                losses,
                draws,
                winRate
            ));
        }

        // Sort by: Wins DESC, Win Rate DESC, Games Played DESC, Username ASC
        entries.sort(Comparator
            .comparingLong(LeaderboardEntryResponse::getWins).reversed()
            .thenComparing(Comparator.comparingDouble(LeaderboardEntryResponse::getWinRate).reversed())
            .thenComparing(Comparator.comparingLong(LeaderboardEntryResponse::getGamesPlayed).reversed())
            .thenComparing(LeaderboardEntryResponse::getUsername, String.CASE_INSENSITIVE_ORDER)
        );

        // Assign ranks and apply limit
        List<LeaderboardEntryResponse> ranked = new ArrayList<>();
        int currentRank = 1;
        for (LeaderboardEntryResponse entry : entries) {
            entry.setRank(currentRank++);
            ranked.add(entry);
            if (ranked.size() >= maxResults) {
                break;
            }
        }

        return ranked;
    }
}
