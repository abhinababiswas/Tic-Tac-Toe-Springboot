package com.tictactoe.model;

import com.tictactoe.exception.InvalidBoardException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a 3x3 Tic-Tac-Toe board.
 * 
 * Logical index mapping:
 * Index 0 -> Position 1
 * Index 1 -> Position 2
 * Index 2 -> Position 3
 * Index 3 -> Position 4
 * Index 4 -> Position 5
 * Index 5 -> Position 6
 * Index 6 -> Position 7
 * Index 7 -> Position 8
 * Index 8 -> Position 9
 */
public class Board {

    public static final int BOARD_SIZE = 9;
    public static final String PLAYER_SYMBOL = "X";
    public static final String COMPUTER_SYMBOL = "O";
    public static final String EMPTY_SYMBOL = "";

    private final List<String> cells;

    public Board() {
        this.cells = new ArrayList<>(Collections.nCopies(BOARD_SIZE, EMPTY_SYMBOL));
    }

    public Board(List<String> rawCells) {
        if (rawCells == null || rawCells.size() != BOARD_SIZE) {
            throw new InvalidBoardException("Board must contain exactly " + BOARD_SIZE + " cells.");
        }

        List<String> normalized = new ArrayList<>(BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE; i++) {
            String cell = rawCells.get(i);
            if (cell == null || cell.trim().isEmpty()) {
                normalized.add(EMPTY_SYMBOL);
            } else if (PLAYER_SYMBOL.equalsIgnoreCase(cell.trim())) {
                normalized.add(PLAYER_SYMBOL);
            } else if (COMPUTER_SYMBOL.equalsIgnoreCase(cell.trim())) {
                normalized.add(COMPUTER_SYMBOL);
            } else {
                throw new InvalidBoardException("Invalid symbol '" + cell + "' at position " + (i + 1) + ". Only 'X', 'O', or empty are permitted.");
            }
        }
        this.cells = normalized;
    }

    public List<String> getCells() {
        return Collections.unmodifiableList(cells);
    }

    public String getCell(int index) {
        validateIndex(index);
        return cells.get(index);
    }

    public boolean isCellEmpty(int index) {
        validateIndex(index);
        return EMPTY_SYMBOL.equals(cells.get(index));
    }

    public List<Integer> getEmptyIndices() {
        List<Integer> emptyIndices = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (EMPTY_SYMBOL.equals(cells.get(i))) {
                emptyIndices.add(i);
            }
        }
        return emptyIndices;
    }

    public int countSymbol(String symbol) {
        int count = 0;
        for (String cell : cells) {
            if (symbol.equals(cell)) {
                count++;
            }
        }
        return count;
    }

    public boolean isFull() {
        for (String cell : cells) {
            if (EMPTY_SYMBOL.equals(cell)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new Board reflecting the move at the specified index.
     * Original board instance remains untouched (immutable transformation).
     */
    public Board withMove(int index, String symbol) {
        validateIndex(index);
        List<String> newCells = new ArrayList<>(this.cells);
        newCells.set(index, symbol);
        return new Board(newCells);
    }

    public static int positionToIndex(int position) {
        if (position < 1 || position > BOARD_SIZE) {
            throw new IllegalArgumentException("Position must be between 1 and " + BOARD_SIZE + ".");
        }
        return position - 1;
    }

    public static int indexToPosition(int index) {
        validateIndex(index);
        return index + 1;
    }

    private static void validateIndex(int index) {
        if (index < 0 || index >= BOARD_SIZE) {
            throw new IllegalArgumentException("Index must be between 0 and " + (BOARD_SIZE - 1) + ".");
        }
    }
}
