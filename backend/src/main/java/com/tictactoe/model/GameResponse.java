package com.tictactoe.model;

import java.util.List;

/**
 * DTO representing the authoritative response returned to the frontend after processing a move.
 * Contains the updated board, resulting game status, optional message, next turn indicator, and computer move position.
 */
public class GameResponse {

    private List<String> board;
    private GameStatus status;
    private String message;
    private String nextTurn;
    private Integer computerMove;

    public GameResponse() {
    }

    public GameResponse(List<String> board, GameStatus status, String message) {
        this(board, status, message, status == GameStatus.IN_PROGRESS ? "PLAYER" : null, null);
    }

    public GameResponse(List<String> board, GameStatus status, String message, String nextTurn, Integer computerMove) {
        this.board = board;
        this.status = status;
        this.message = message;
        this.nextTurn = nextTurn;
        this.computerMove = computerMove;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNextTurn() {
        return nextTurn;
    }

    public void setNextTurn(String nextTurn) {
        this.nextTurn = nextTurn;
    }

    public Integer getComputerMove() {
        return computerMove;
    }

    public void setComputerMove(Integer computerMove) {
        this.computerMove = computerMove;
    }
}
