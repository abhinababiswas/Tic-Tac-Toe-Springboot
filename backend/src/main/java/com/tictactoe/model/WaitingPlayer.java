package com.tictactoe.model;

import java.time.Instant;

/**
 * In-memory representation of a player waiting in the matchmaking queue.
 */
public class WaitingPlayer {

    private final Long userId;
    private final String username;
    private final String stompSessionId;
    private final String principalName;
    private final Instant queuedAt;

    public WaitingPlayer(Long userId, String username, String stompSessionId, String principalName) {
        this.userId = userId;
        this.username = username;
        this.stompSessionId = stompSessionId;
        this.principalName = principalName;
        this.queuedAt = Instant.now();
    }

    public WaitingPlayer(Long userId, String username, String stompSessionId) {
        this(userId, username, stompSessionId, stompSessionId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getStompSessionId() {
        return stompSessionId;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }
}
