package com.tictactoe.exception;

/**
 * Exception thrown when a move violates Tic-Tac-Toe rules or board constraints.
 */
public class InvalidMoveException extends RuntimeException {

    public InvalidMoveException(String message) {
        super(message);
    }
}
