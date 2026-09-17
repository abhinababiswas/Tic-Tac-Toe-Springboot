package com.tictactoe.exception;

/**
 * Thrown when a requested resource (user, game record) cannot be found in the database.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
