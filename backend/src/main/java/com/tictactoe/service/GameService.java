package com.tictactoe.service;

import com.tictactoe.engine.ComputerPlayer;
import com.tictactoe.engine.GameEngine;
import com.tictactoe.exception.InvalidBoardException;
import com.tictactoe.exception.InvalidMoveException;
import com.tictactoe.model.Board;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.GameResponse;
import com.tictactoe.model.GameStatus;
import com.tictactoe.model.MoveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating Tic-Tac-Toe turn progression.
 * Stateless coordinator between request DTOs, game engine rules, and AI decision-making.
 * Contains no HTTP, REST, or UI dependencies.
 */
@Service
public class GameService {

    private final GameEngine gameEngine;
    private final ComputerPlayer computerPlayer;

    @Autowired
    public GameService(GameEngine gameEngine, ComputerPlayer computerPlayer) {
        this.gameEngine = gameEngine;
        this.computerPlayer = computerPlayer;
    }

    /**
     * Executes an authoritative game turn:
     * 1. Validate board and player move legality.
     * 2. Apply player's move ('X').
     * 3. Check for Player Win or Draw.
     * 4. If IN_PROGRESS, compute and apply computer move ('O').
     * 5. Check for Computer Win or Draw.
     * 6. Return updated board state and status.
     *
     * @param request payload containing board, player move position (1-9), and difficulty
     * @return authoritative GameResponse
     */
    public GameResponse processMove(MoveRequest request) {
        if (request == null) {
            throw new InvalidMoveException("Move request must not be null.");
        }
        if (request.getBoard() == null) {
            throw new InvalidBoardException("Board must not be null.");
        }

        // 1. Build and validate board structure
        Board board = new Board(request.getBoard());

        // 2. Validate board turn consistency & ensure game is not already concluded
        gameEngine.validateBoard(board);

        // 3. Validate player move position (1-9, cell empty)
        gameEngine.validateMove(board, request.getPosition());

        // 4. Apply player's move
        int playerIndex = Board.positionToIndex(request.getPosition());
        board = board.withMove(playerIndex, Board.PLAYER_SYMBOL);

        // 5. Evaluate result after player move
        if (gameEngine.checkWinner(board, Board.PLAYER_SYMBOL)) {
            return new GameResponse(board.getCells(), GameStatus.PLAYER_WON, "Player wins!", null, null);
        }

        if (board.isFull()) {
            return new GameResponse(board.getCells(), GameStatus.DRAW, "Game ended in a draw.", null, null);
        }

        // 6. Compute and apply computer move
        Difficulty difficulty = request.getDifficulty() != null ? request.getDifficulty() : Difficulty.MEDIUM;
        int computerIndex = computerPlayer.chooseMove(board, difficulty);
        int computerPosition = Board.indexToPosition(computerIndex);
        board = board.withMove(computerIndex, Board.COMPUTER_SYMBOL);

        // 7. Evaluate result after computer move
        if (gameEngine.checkWinner(board, Board.COMPUTER_SYMBOL)) {
            return new GameResponse(
                board.getCells(),
                GameStatus.COMPUTER_WON,
                "Computer played position " + computerPosition + " and won!",
                null,
                computerPosition
            );
        }

        if (board.isFull()) {
            return new GameResponse(
                board.getCells(),
                GameStatus.DRAW,
                "Computer played position " + computerPosition + ". Game ended in a draw.",
                null,
                computerPosition
            );
        }

        return new GameResponse(
            board.getCells(),
            GameStatus.IN_PROGRESS,
            "Computer played position " + computerPosition + ". Your turn!",
            "PLAYER",
            computerPosition
        );
    }
}
