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
    private final GameHistoryService gameHistoryService;

    public GameService(GameEngine gameEngine, ComputerPlayer computerPlayer) {
        this(gameEngine, computerPlayer, null);
    }

    @Autowired
    public GameService(GameEngine gameEngine, ComputerPlayer computerPlayer, @Autowired(required = false) GameHistoryService gameHistoryService) {
        this.gameEngine = gameEngine;
        this.computerPlayer = computerPlayer;
        this.gameHistoryService = gameHistoryService;
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

        Difficulty difficulty = request.getDifficulty() != null ? request.getDifficulty() : Difficulty.MEDIUM;

        // 5. Evaluate result after player move
        if (gameEngine.checkWinner(board, Board.PLAYER_SYMBOL)) {
            recordGameIfTerminal(request, difficulty, GameStatus.PLAYER_WON);
            return new GameResponse(board.getCells(), GameStatus.PLAYER_WON, "Player wins!", null, null);
        }

        if (board.isFull()) {
            recordGameIfTerminal(request, difficulty, GameStatus.DRAW);
            return new GameResponse(board.getCells(), GameStatus.DRAW, "Game ended in a draw.", null, null);
        }

        // 6. Compute and apply computer move
        int computerIndex = computerPlayer.chooseMove(board, difficulty);
        int computerPosition = Board.indexToPosition(computerIndex);
        board = board.withMove(computerIndex, Board.COMPUTER_SYMBOL);

        // 7. Evaluate result after computer move
        if (gameEngine.checkWinner(board, Board.COMPUTER_SYMBOL)) {
            recordGameIfTerminal(request, difficulty, GameStatus.COMPUTER_WON);
            return new GameResponse(
                board.getCells(),
                GameStatus.COMPUTER_WON,
                "Computer played position " + computerPosition + " and won!",
                null,
                computerPosition
            );
        }

        if (board.isFull()) {
            recordGameIfTerminal(request, difficulty, GameStatus.DRAW);
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

    private void recordGameIfTerminal(MoveRequest request, Difficulty difficulty, GameStatus status) {
        if (gameHistoryService != null && request.getUserId() != null && status != null && status.isTerminal()) {
            java.time.Instant startedAt = request.getStartedAt() != null ? request.getStartedAt() : java.time.Instant.now();
            gameHistoryService.recordCompletedComputerGame(
                request.getUserId(),
                difficulty,
                status,
                startedAt,
                java.time.Instant.now()
            );
        }
    }
}
