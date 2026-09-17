package com.tictactoe.model;

/**
 * Represents the symbols used on the Tic-Tac-Toe board.
 */
public enum PlayerSymbol {
    X("X"),
    O("O");

    private final String value;

    PlayerSymbol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns the opponent's symbol.
     */
    public PlayerSymbol opponent() {
        return this == X ? O : X;
    }

    /**
     * Resolves a string symbol ("X" or "O") to PlayerSymbol enum.
     */
    public static PlayerSymbol fromString(String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Symbol string must not be null.");
        }
        for (PlayerSymbol ps : values()) {
            if (ps.value.equalsIgnoreCase(symbol.trim())) {
                return ps;
            }
        }
        throw new IllegalArgumentException("Unknown symbol: " + symbol);
    }
}
