package com.tictactoe.dto.websocket;

/**
 * Payload sent by a client to leave the matchmaking queue.
 */
public class LeaveMatchmakingRequest {

    private Long userId;

    public LeaveMatchmakingRequest() {
    }

    public LeaveMatchmakingRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
