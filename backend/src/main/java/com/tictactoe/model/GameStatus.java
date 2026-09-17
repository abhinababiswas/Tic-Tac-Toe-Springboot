package com.tictactoe.model;

/**
 * Represents the lifecycle and outcome status of a Tic-Tac-Toe game.
 * Supports both single-player (vs Computer) and multiplayer modes.
 */
public enum GameStatus {
    // Shared / In-Progress states
    WAITING,        // Multiplayer: Waiting for a second player to join
    MATCHED,        // Multiplayer: Opponents matched, preparing board
    IN_PROGRESS,    // Active gameplay in progress

    // Single-player (Player vs Computer) terminal states
    PLAYER_WON,     // Human player achieved 3 in a row
    COMPUTER_WON,   // Computer achieved 3 in a row

    // Multiplayer terminal states
    PLAYER_ONE_WON, // First player (X) achieved 3 in a row
    PLAYER_TWO_WON, // Second player (O) achieved 3 in a row

    // Shared terminal states
    DRAW,           // Board is full with no winner
    ABANDONED;      // Player disconnected or forfeited

    /**
     * Checks if this status represents a completed or finished game state.
     */
    public boolean isTerminal() {
        return this == PLAYER_WON ||
               this == COMPUTER_WON ||
               this == PLAYER_ONE_WON ||
               this == PLAYER_TWO_WON ||
               this == DRAW ||
               this == ABANDONED;
    }
}
