package com.tictactoe.dto.websocket;

/**
 * Payload sent by a client to join the matchmaking queue.
 */
public class JoinMatchmakingRequest {

    private Long userId;
    private String username;

    public JoinMatchmakingRequest() {
    }

    public JoinMatchmakingRequest(Long userId, String username) {
        this.userId = userId;
        this.username = username;
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
}
