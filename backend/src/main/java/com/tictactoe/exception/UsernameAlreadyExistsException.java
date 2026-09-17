package com.tictactoe.exception;

/**
 * Thrown when an attempt is made to register a username that is already taken.
 */
public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
