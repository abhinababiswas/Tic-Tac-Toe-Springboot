package com.tictactoe.dto.websocket;

import java.time.Instant;

/**
 * Structured error message sent to /user/queue/errors when an invalid move
 * or illegal action occurs. Never exposes internal stack traces.
 */
public class WebSocketErrorMessage {

    private String errorCode;
    private String errorMessage;
    private Instant timestamp;

    public WebSocketErrorMessage() {
        this.timestamp = Instant.now();
    }

    public WebSocketErrorMessage(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
