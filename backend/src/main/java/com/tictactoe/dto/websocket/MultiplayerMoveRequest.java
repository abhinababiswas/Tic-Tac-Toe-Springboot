package com.tictactoe.dto.websocket;

/**
 * Payload sent by a player to make a move in an active multiplayer game.
 */
public class MultiplayerMoveRequest {

    private int position;
    private Long userId;

    public MultiplayerMoveRequest() {
    }

    public MultiplayerMoveRequest(int position, Long userId) {
        this.position = position;
        this.userId = userId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
