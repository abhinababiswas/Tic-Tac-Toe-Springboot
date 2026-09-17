package com.tictactoe.model;

import com.tictactoe.engine.GameEngine;
import com.tictactoe.exception.InvalidMoveException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe runtime state holder for an active online multiplayer match.
 * Reuses the pure domain Board and GameEngine without adding JPA annotations.
 */
public class MultiplayerGameSession {

    private final String gameId;
    private final WaitingPlayer playerX;
    private final WaitingPlayer playerO;
    private Board board;
    private final Instant startedAt;

    private PlayerSymbol currentTurn;
    private GameStatus status;
    private Instant completedAt;
    private PlayerSymbol winner;
    private List<Integer> winningLine;
    private Integer lastMovePosition;
    private PlayerSymbol lastMovePlayer;

    public MultiplayerGameSession(String gameId, WaitingPlayer playerX, WaitingPlayer playerO) {
        this.gameId = gameId;
        this.playerX = playerX;
        this.playerO = playerO;
        this.board = new Board();
        this.startedAt = Instant.now();
        this.currentTurn = PlayerSymbol.X; // 'X' always moves first
        this.status = GameStatus.IN_PROGRESS;
    }

    /**
     * Synchronously validates and executes a player's move.
     * Guaranteed thread-safe against concurrent or simultaneous move requests.
     */
    public synchronized void applyMove(Long userId, int position, GameEngine gameEngine) {
        if (status.isTerminal()) {
            throw new InvalidMoveException("Cannot make a move: Game is already finished with status " + status);
        }

        PlayerSymbol playerSymbol = getPlayerSymbol(userId);
        if (playerSymbol == null) {
            throw new InvalidMoveException("User " + userId + " is not a participant in game " + gameId);
        }

        if (currentTurn != playerSymbol) {
            throw new InvalidMoveException("It is not your turn. Current turn: " + currentTurn);
        }

        gameEngine.validateMove(board, position);

        int index = Board.positionToIndex(position);
        this.board = this.board.withMove(index, playerSymbol.name());
        this.lastMovePosition = position;
        this.lastMovePlayer = playerSymbol;

        if (gameEngine.checkWinner(board, playerSymbol.name())) {
            this.status = (playerSymbol == PlayerSymbol.X) ? GameStatus.PLAYER_ONE_WON : GameStatus.PLAYER_TWO_WON;
            this.winner = playerSymbol;
            this.winningLine = extractWinningLine(playerSymbol.name());
            this.completedAt = Instant.now();
        } else if (board.isFull()) {
            this.status = GameStatus.DRAW;
            this.completedAt = Instant.now();
        } else {
            this.currentTurn = (playerSymbol == PlayerSymbol.X) ? PlayerSymbol.O : PlayerSymbol.X;
        }
    }

    /**
     * Marks the match as abandoned if a player disconnects during active play.
     */
    public synchronized boolean markAbandoned() {
        if (this.status == GameStatus.IN_PROGRESS) {
            this.status = GameStatus.ABANDONED;
            this.completedAt = Instant.now();
            return true;
        }
        return false;
    }

    public PlayerSymbol getPlayerSymbol(Long userId) {
        if (userId == null) {
            return null;
        }
        if (playerX.getUserId() != null && playerX.getUserId().equals(userId)) {
            return PlayerSymbol.X;
        }
        if (playerO.getUserId() != null && playerO.getUserId().equals(userId)) {
            return PlayerSymbol.O;
        }
        return null;
    }

    public boolean hasParticipant(Long userId) {
        return getPlayerSymbol(userId) != null;
    }

    private List<Integer> extractWinningLine(String symbol) {
        for (int[] combo : GameEngine.WINNING_COMBINATIONS) {
            if (symbol.equals(board.getCell(combo[0])) &&
                symbol.equals(board.getCell(combo[1])) &&
                symbol.equals(board.getCell(combo[2]))) {
                List<Integer> line = new ArrayList<>();
                line.add(combo[0] + 1); // 1-based positions
                line.add(combo[1] + 1);
                line.add(combo[2] + 1);
                return line;
            }
        }
        return null;
    }

    public String getGameId() {
        return gameId;
    }

    public WaitingPlayer getPlayerX() {
        return playerX;
    }

    public WaitingPlayer getPlayerO() {
        return playerO;
    }

    public Board getBoard() {
        return board;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public synchronized PlayerSymbol getCurrentTurn() {
        return currentTurn;
    }

    public synchronized GameStatus getStatus() {
        return status;
    }

    public synchronized Instant getCompletedAt() {
        return completedAt;
    }

    public synchronized PlayerSymbol getWinner() {
        return winner;
    }

    public synchronized List<Integer> getWinningLine() {
        return winningLine != null ? new ArrayList<>(winningLine) : null;
    }

    public synchronized Integer getLastMovePosition() {
        return lastMovePosition;
    }

    public synchronized PlayerSymbol getLastMovePlayer() {
        return lastMovePlayer;
    }
}
