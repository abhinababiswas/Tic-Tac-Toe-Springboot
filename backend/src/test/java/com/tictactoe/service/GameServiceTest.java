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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        GameEngine gameEngine = new GameEngine();
        ComputerPlayer computerPlayer = new ComputerPlayer(gameEngine);
        gameService = new GameService(gameEngine, computerPlayer);
    }

    @Test
    @DisplayName("Player winning move terminates turn with PLAYER_WON; computer does not move")
    void shouldConcludeTurnWhenPlayerWins() {
        // X X _  (Player plays position 3 / index 2 -> Player wins!)
        // O O _
        // _ _ _
        List<String> cells = Arrays.asList(
            "X", "X", "",
            "O", "O", "",
            "",  "",  ""
        );
        MoveRequest request = new MoveRequest(cells, 3, Difficulty.MEDIUM);

        GameResponse response = gameService.processMove(request);

        assertEquals(GameStatus.PLAYER_WON, response.getStatus());
        assertEquals("X", response.getBoard().get(2));
        // O should not have made a move
        assertEquals("", response.getBoard().get(5));
        assertEquals("Player wins!", response.getMessage());
    }

    @Test
    @DisplayName("Player move filling the last cell terminates turn with DRAW; computer does not move")
    void shouldConcludeTurnWithDrawWhenPlayerFillsLastCell() {
        // X O X
        // X O O
        // O X _  (Player plays position 9 / index 8 -> Draw!)
        List<String> cells = Arrays.asList(
            "X", "O", "X",
            "X", "O", "O",
            "O", "X", ""
        );
        // Note: Equal count before turn: 4 X's, 4 O's
        MoveRequest request = new MoveRequest(cells, 9, Difficulty.MEDIUM);

        GameResponse response = gameService.processMove(request);

        assertEquals(GameStatus.DRAW, response.getStatus());
        assertEquals("X", response.getBoard().get(8));
        assertTrue(response.getBoard().stream().noneMatch(String::isEmpty));
    }

    @Test
    @DisplayName("Regular move applies player X and computer O; returns IN_PROGRESS")
    void shouldProcessRegularTurnWithComputerResponse() {
        // Fresh board, player plays center (position 5 / index 4)
        Board initialBoard = new Board();
        MoveRequest request = new MoveRequest(initialBoard.getCells(), 5, Difficulty.MEDIUM);

        GameResponse response = gameService.processMove(request);

        assertEquals(GameStatus.IN_PROGRESS, response.getStatus());
        assertEquals("X", response.getBoard().get(4));

        // Computer must have placed one 'O'
        long oCount = response.getBoard().stream().filter("O"::equals).count();
        assertEquals(1, oCount, "Computer must have played exactly one move.");
    }

    @Test
    @DisplayName("Computer winning move results in COMPUTER_WON")
    void shouldDetectComputerWin() {
        List<String> cells = Arrays.asList(
            "O", "O", "",
            "X", "",  "",
            "X", "",  ""
        );
        // Player plays position 5 (index 4)
        MoveRequest request = new MoveRequest(cells, 5, Difficulty.MEDIUM);

        GameResponse response = gameService.processMove(request);

        // Computer takes position 3 (index 2) to win!
        assertEquals(GameStatus.COMPUTER_WON, response.getStatus());
        assertEquals("O", response.getBoard().get(2));
        assertTrue(response.getMessage().contains("Computer played position 3 and won!"));
    }

    @Test
    @DisplayName("Rejects null move request")
    void shouldRejectNullRequest() {
        assertThrows(InvalidMoveException.class, () -> gameService.processMove(null));
    }

    @Test
    @DisplayName("Rejects move on occupied position")
    void shouldRejectMoveOnOccupiedCell() {
        List<String> cells = Arrays.asList(
            "X", "O", "",
            "",  "",  "",
            "",  "",  ""
        );
        // Player tries to play position 1 (occupied by X)
        MoveRequest request = new MoveRequest(cells, 1, Difficulty.MEDIUM);

        assertThrows(InvalidMoveException.class, () -> gameService.processMove(request));
    }

    @Test
    @DisplayName("Rejects board with invalid turn count")
    void shouldRejectInvalidTurnCountBoard() {
        // 3 X's and 1 O -> invalid before player move
        List<String> cells = Arrays.asList(
            "X", "X", "X",
            "O", "",  "",
            "",  "",  ""
        );
        MoveRequest request = new MoveRequest(cells, 5, Difficulty.MEDIUM);

        assertThrows(InvalidBoardException.class, () -> gameService.processMove(request));
    }

    @Test
    @DisplayName("Rejects move if computer has already won on the board")
    void shouldRejectMoveIfComputerAlreadyWon() {
        // Equal counts: 3 X's, 3 O's (O won on top row)
        List<String> cells = Arrays.asList(
            "O", "O", "O",
            "X", "X", "",
            "X", "",  ""
        );
        MoveRequest request = new MoveRequest(cells, 6, Difficulty.MEDIUM);
        assertThrows(InvalidMoveException.class, () -> gameService.processMove(request));
    }

    @Test
    @DisplayName("Rejects move if board is already full (draw)")
    void shouldRejectMoveIfBoardIsAlreadyFull() {
        List<String> cells = Arrays.asList(
            "X", "O", "X",
            "X", "O", "O",
            "O", "X", "X"
        );
        MoveRequest request = new MoveRequest(cells, 1, Difficulty.MEDIUM);
        assertThrows(InvalidBoardException.class, () -> gameService.processMove(request));
    }

    @Test
    @DisplayName("Processes move when difficulty is HARD using Minimax strategy")
    void shouldProcessMoveWhenDifficultyIsHard() {
        // Player plays center (position 5) with HARD difficulty
        List<String> cells = Arrays.asList(
            "", "", "",
            "", "", "",
            "", "", ""
        );
        MoveRequest request = new MoveRequest(cells, 5, Difficulty.HARD);
        GameResponse response = gameService.processMove(request);

        assertNotNull(response);
        assertEquals(GameStatus.IN_PROGRESS, response.getStatus());
        assertEquals("X", response.getBoard().get(4)); // center position
        assertNotNull(response.getComputerMove());
        // Computer responded with a corner (0, 2, 6, or 8)
        List<Integer> validCorners = Arrays.asList(1, 3, 7, 9); // 1-based positions
        assertTrue(validCorners.contains(response.getComputerMove()),
            "Against center move, Hard AI should take a corner: " + response.getComputerMove());
    }
}
