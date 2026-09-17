package com.tictactoe.repository;

/**
 * Projection interface for aggregated player statistics used in leaderboard calculations.
 */
public interface PlayerStatProjection {

    Long getPlayerId();

    String getUsername();

    Long getGamesPlayed();

    Long getWins();

    Long getLosses();

    Long getDraws();
}
