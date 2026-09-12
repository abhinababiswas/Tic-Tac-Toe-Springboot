package com.tictactoe.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * DTO representing an incoming move request from the frontend.
 * The backend is stateless; the frontend supplies the current board state,
 * the player's chosen position (1-9), and the selected difficulty level.
 */
public class MoveRequest {

    private List<String> board;

    @JsonAlias({"move", "cell"})
    private Integer position;

    private Difficulty difficulty;

    public MoveRequest() {
    }

    public MoveRequest(List<String> board, Integer position, Difficulty difficulty) {
        this.board = board;
        this.position = position;
        this.difficulty = difficulty;
    }

    public List<String> getBoard() {
        return board;
    }

    public void setBoard(List<String> board) {
        this.board = board;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getMove() {
        return position;
    }

    public void setMove(Integer move) {
        this.position = move;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
}
