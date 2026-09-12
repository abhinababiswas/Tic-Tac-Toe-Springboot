package com.tictactoe.engine;

import com.tictactoe.exception.InvalidBoardException;
import com.tictactoe.exception.InvalidMoveException;
import com.tictactoe.model.Board;
import com.tictactoe.model.GameStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Core Tic-Tac-Toe game engine.
 * Pure domain logic for win/draw detection, move validation, and move simulation.
 * Contains no HTTP, UI, or framework-specific presentation concerns.
 */
@Component
public class GameEngine {

    /**
     * All 8 possible winning lines on a 3x3 board:
     * - 3 Rows: {0,1,2}, {3,4,5}, {6,7,8}
     * - 3 Columns: {0,3,6}, {1,4,7}, {2,5,8}
     * - 2 Diagonals: {0,4,8}, {2,4,6}
     */
    public static final int[][] WINNING_COMBINATIONS = {
        {0, 1, 2},
        {3, 4, 5},
        {6, 7, 8},
        {0, 3, 6},
        {1, 4, 7},
        {2, 5, 8},
        {0, 4, 8},
        {2, 4, 6}
    };

    /**
     * Checks if the specified player symbol has achieved 3 in a row.
     */
    public boolean checkWinner(Board board, String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        for (int[] combo : WINNING_COMBINATIONS) {
            if (symbol.equals(board.getCell(combo[0])) &&
                symbol.equals(board.getCell(combo[1])) &&
                symbol.equals(board.getCell(combo[2]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identifies the winner if one exists on the board.
     */
    public Optional<String> getWinner(Board board) {
        if (checkWinner(board, Board.PLAYER_SYMBOL)) {
            return Optional.of(Board.PLAYER_SYMBOL);
        }
        if (checkWinner(board, Board.COMPUTER_SYMBOL)) {
            return Optional.of(Board.COMPUTER_SYMBOL);
        }
        return Optional.empty();
    }

    /**
     * Checks if the game has ended in a draw (full board and no winner).
     */
    public boolean checkDraw(Board board) {
        return board.isFull() && getWinner(board).isEmpty();
    }

    /**
     * Determines the current game status from the board state.
     */
    public GameStatus determineStatus(Board board) {
        if (checkWinner(board, Board.PLAYER_SYMBOL)) {
            return GameStatus.PLAYER_WON;
        }
        if (checkWinner(board, Board.COMPUTER_SYMBOL)) {
            return GameStatus.COMPUTER_WON;
        }
        if (board.isFull()) {
            return GameStatus.DRAW;
        }
        return GameStatus.IN_PROGRESS;
    }

    /**
     * Simulates placing a symbol at the specified index to determine if it produces an immediate win.
     * Uses immutable Board transformation; does not alter the original board.
     */
    public boolean isWinningMove(Board board, int index, String symbol) {
        if (!board.isCellEmpty(index)) {
            return false;
        }
        Board simulated = board.withMove(index, symbol);
        return checkWinner(simulated, symbol);
    }

    /**
     * Finds the first available cell index that would result in an immediate win for the given symbol.
     *
     * @param board current board state
     * @param symbol player or computer symbol
     * @return 0-based index of winning move, or empty if none exists
     */
    public OptionalInt findWinningMoveIndex(Board board, String symbol) {
        for (int index : board.getEmptyIndices()) {
            if (isWinningMove(board, index, symbol)) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Validates that the incoming board represents a valid, non-terminal state for a player move.
     */
    public void validateBoard(Board board) {
        int xCount = board.countSymbol(Board.PLAYER_SYMBOL);
        int oCount = board.countSymbol(Board.COMPUTER_SYMBOL);

        // Player always plays first and turns alternate; when receiving a move request, X count must equal O count
        if (xCount != oCount) {
            throw new InvalidBoardException(
                "Invalid board state: expected equal count of 'X' and 'O' before player's turn, but found " +
                xCount + " 'X' and " + oCount + " 'O'."
            );
        }

        // Cannot accept a move if the board is already in a terminal state
        if (checkWinner(board, Board.PLAYER_SYMBOL)) {
            throw new InvalidMoveException("Cannot make a move: Player has already won.");
        }
        if (checkWinner(board, Board.COMPUTER_SYMBOL)) {
            throw new InvalidMoveException("Cannot make a move: Computer has already won.");
        }
        if (board.isFull()) {
            throw new InvalidMoveException("Cannot make a move: Board is full (game is a draw).");
        }
    }

    /**
     * Validates that the player's chosen move position is within bounds and not occupied.
     *
     * @param board current board state
     * @param position 1-based position (1-9)
     */
    public void validateMove(Board board, Integer position) {
        if (position == null) {
            throw new InvalidMoveException("Move position must not be null.");
        }
        if (position < 1 || position > Board.BOARD_SIZE) {
            throw new InvalidMoveException(
                "Move position " + position + " is invalid. Must be between 1 and " + Board.BOARD_SIZE + "."
            );
        }

        int index = Board.positionToIndex(position);
        if (!board.isCellEmpty(index)) {
            throw new InvalidMoveException(
                "Position " + position + " is already occupied by '" + board.getCell(index) + "'."
            );
        }
    }
}
