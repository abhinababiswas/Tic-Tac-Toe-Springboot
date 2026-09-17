package com.tictactoe.dto.websocket;

import com.tictactoe.model.GameStatus;
import com.tictactoe.model.PlayerSymbol;

import java.util.List;

/**
 * Message broadcast to all participants of a game (/topic/game/{gameId})
 * reflecting server-authoritative state changes, turn shifts, and completions.
 */
public class GameStateMessage {

    private String type; // GAME_START, GAME_UPDATE, GAME_FINISHED, GAME_ABANDONED
    private String gameId;
    private List<String> board;
    private PlayerSymbol currentTurn;
    private GameStatus status;
    private String message;
    private Integer lastMovePosition;
    private PlayerSymbol lastMovePlayer;
    private PlayerSymbol winner;
    private List<Integer> winningLine;

    public GameStateMessage() {
    }

    public GameStateMessage(String type, String gameId, List<String> board, PlayerSymbol currentTurn,
                            GameStatus status, String message, Integer lastMovePosition,
                            PlayerSymbol lastMovePlayer, PlayerSymbol winner, List<Integer> winningLine) {
        this.type = type;
        this.gameId = gameId;
        this.board = board;
        this.currentTurn = currentTurn;
        this.status = status;
        this.message = message;
        this.lastMovePosition = lastMovePosition;
        this.lastMovePlayer = lastMovePlayer;
        this.winner = winner;
        this.winningLine = winningLine;
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

    public List<String> getBoard() {
        return board;
    }

    public void setBoard(List<String> board) {
        this.board = board;
    }

    public PlayerSymbol getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PlayerSymbol currentTurn) {
        this.currentTurn = currentTurn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getLastMovePosition() {
        return lastMovePosition;
    }

    public void setLastMovePosition(Integer lastMovePosition) {
        this.lastMovePosition = lastMovePosition;
    }

    public PlayerSymbol getLastMovePlayer() {
        return lastMovePlayer;
    }

    public void setLastMovePlayer(PlayerSymbol lastMovePlayer) {
        this.lastMovePlayer = lastMovePlayer;
    }

    public PlayerSymbol getWinner() {
        return winner;
    }

    public void setWinner(PlayerSymbol winner) {
        this.winner = winner;
    }

    public List<Integer> getWinningLine() {
        return winningLine;
    }

    public void setWinningLine(List<Integer> winningLine) {
        this.winningLine = winningLine;
    }
}
