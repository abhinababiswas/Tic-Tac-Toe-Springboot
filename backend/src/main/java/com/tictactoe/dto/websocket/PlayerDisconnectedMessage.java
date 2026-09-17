package com.tictactoe.dto.websocket;

/**
 * Broadcast to /topic/game/{gameId} when an active participant disconnects.
 */
public class PlayerDisconnectedMessage {

    private String type = "PLAYER_DISCONNECTED";
    private String gameId;
    private String message;

    public PlayerDisconnectedMessage() {
    }

    public PlayerDisconnectedMessage(String gameId, String message) {
        this.gameId = gameId;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
