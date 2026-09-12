package com.tictactoe.exception;

/**
 * Exception thrown when an incoming board representation fails structural or game-rule validation.
 */
public class InvalidBoardException extends RuntimeException {

    public InvalidBoardException(String message) {
        super(message);
    }
}
