package com.tictactoe.dto;

/**
 * DTO representing an entry on the competitive leaderboard.
 */
public class LeaderboardEntryResponse {

    private int rank;
    private Long userId;
    private String username;
    private long gamesPlayed;
    private long wins;
    private long losses;
    private long draws;
    private double winRate;

    public LeaderboardEntryResponse() {
    }

    public LeaderboardEntryResponse(int rank, Long userId, String username, long gamesPlayed, long wins, long losses, long draws, double winRate) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.gamesPlayed = gamesPlayed;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.winRate = winRate;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(long gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public long getWins() {
        return wins;
    }

    public void setWins(long wins) {
        this.wins = wins;
    }

    public long getLosses() {
        return losses;
    }

    public void setLosses(long losses) {
        this.losses = losses;
    }

    public long getDraws() {
        return draws;
    }

    public void setDraws(long draws) {
        this.draws = draws;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
}
