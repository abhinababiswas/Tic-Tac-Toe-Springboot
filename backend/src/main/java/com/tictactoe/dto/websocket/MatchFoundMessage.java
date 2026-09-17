package com.tictactoe.dto.websocket;

import com.tictactoe.model.GameStatus;
import com.tictactoe.model.PlayerSymbol;

import java.util.List;

/**
 * Message sent to a player's private user queue (/user/queue/match) when paired with an opponent.
 */
public class MatchFoundMessage {

    private String type = "MATCH_FOUND";
    private String gameId;
    private PlayerSymbol yourSymbol;
    private String opponentUsername;
    private PlayerSymbol currentTurn;
    private List<String> board;
    private GameStatus status;

    public MatchFoundMessage() {
    }

    public MatchFoundMessage(String gameId, PlayerSymbol yourSymbol, String opponentUsername,
                             PlayerSymbol currentTurn, List<String> board, GameStatus status) {
        this.gameId = gameId;
        this.yourSymbol = yourSymbol;
        this.opponentUsername = opponentUsername;
        this.currentTurn = currentTurn;
        this.board = board;
        this.status = status;
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

    public PlayerSymbol getYourSymbol() {
        return yourSymbol;
    }

    public void setYourSymbol(PlayerSymbol yourSymbol) {
        this.yourSymbol = yourSymbol;
    }

    public String getOpponentUsername() {
        return opponentUsername;
    }

    public void setOpponentUsername(String opponentUsername) {
        this.opponentUsername = opponentUsername;
    }

    public PlayerSymbol getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PlayerSymbol currentTurn) {
        this.currentTurn = currentTurn;
    }

    public List<String> getBoard() {
        return board;
    }

    public void setBoard(List<String> board) {
        this.board = board;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }
}
