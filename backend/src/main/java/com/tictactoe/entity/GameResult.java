package com.tictactoe.entity;

/**
 * Server-authoritative result of a completed game.
 * Distinguishes outcomes cleanly from game lifecycle status.
 */
public enum GameResult {
    PLAYER_WIN,       // Human player won against computer
    COMPUTER_WIN,     // Computer won against human player
    DRAW,             // Game ended in a draw with no winner
    PLAYER_ONE_WIN,   // Player 1 won (reserved for multiplayer)
    PLAYER_TWO_WIN    // Player 2 won (reserved for multiplayer)
}
