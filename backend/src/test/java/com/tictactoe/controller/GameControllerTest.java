package com.tictactoe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.MoveRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Health endpoint returns 200 UP")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/game/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.service", is("tictactoe-backend")));
    }

    @Nested
    @DisplayName("Successful API Moves")
    class SuccessfulMoveTests {

        @Test
        @DisplayName("Easy mode move returns 200 and IN_PROGRESS state")
        void shouldProcessEasyModeMove() throws Exception {
            List<String> board = Arrays.asList(
                "", "", "",
                "", "", "",
                "", "", ""
            );
            MoveRequest request = new MoveRequest(board, 5, Difficulty.EASY);

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.board[4]", is("X")))
                .andExpect(jsonPath("$.nextTurn", is("PLAYER")))
                .andExpect(jsonPath("$.computerMove", notNullValue()))
                .andExpect(jsonPath("$.board", hasSize(9)));
        }

        @Test
        @DisplayName("Medium mode move returns 200 and applies player and computer moves")
        void shouldProcessMediumModeMove() throws Exception {
            List<String> board = Arrays.asList(
                "", "", "",
                "", "", "",
                "", "", ""
            );
            MoveRequest request = new MoveRequest(board, 1, Difficulty.MEDIUM);

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.board[0]", is("X")))
                .andExpect(jsonPath("$.computerMove", notNullValue()));
        }

        @Test
        @DisplayName("Supports JSON field 'move' as alias for 'position'")
        void shouldSupportMoveAlias() throws Exception {
            String jsonPayload = """
                {
                    "board": ["", "", "", "", "", "", "", "", ""],
                    "move": 5,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board[4]", is("X")));
        }
    }

    @Nested
    @DisplayName("Game Outcome API Tests")
    class GameOutcomeTests {

        @Test
        @DisplayName("Player winning move returns PLAYER_WON and computer does not play")
        void shouldReturnPlayerWon() throws Exception {
            // X X _
            // O O _
            // _ _ _
            List<String> board = Arrays.asList(
                "X", "X", "",
                "O", "O", "",
                "",  "",  ""
            );
            MoveRequest request = new MoveRequest(board, 3, Difficulty.MEDIUM);

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PLAYER_WON")))
                .andExpect(jsonPath("$.board[2]", is("X")))
                .andExpect(jsonPath("$.computerMove", nullValue()))
                .andExpect(jsonPath("$.nextTurn", nullValue()));
        }

        @Test
        @DisplayName("Computer winning move returns COMPUTER_WON")
        void shouldReturnComputerWon() throws Exception {
            // O O _ (computer can win on position 3 / index 2)
            // X _ _
            // X _ _
            List<String> board = Arrays.asList(
                "O", "O", "",
                "X", "",  "",
                "X", "",  ""
            );
            // Player plays position 5 (index 4)
            MoveRequest request = new MoveRequest(board, 5, Difficulty.MEDIUM);

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPUTER_WON")))
                .andExpect(jsonPath("$.board[4]", is("X")))
                .andExpect(jsonPath("$.board[2]", is("O")))
                .andExpect(jsonPath("$.computerMove", is(3)));
        }

        @Test
        @DisplayName("Critical Medium Priority Test: Computer chooses WIN over BLOCK")
        void shouldPrioritizeWinOverBlock() throws Exception {
            // O O _ (pos 3 / index 2 wins for O)
            // X X _ (pos 6 / index 5 wins for X)
            // _ _ _ (X count: 2, O count: 2)
            List<String> board = Arrays.asList(
                "O", "O", "",
                "X", "X", "",
                "",  "",  ""
            );
            // Player plays position 8 (index 7)
            MoveRequest request = new MoveRequest(board, 8, Difficulty.MEDIUM);

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPUTER_WON")))
                .andExpect(jsonPath("$.computerMove", is(3)))
                .andExpect(jsonPath("$.board[2]", is("O")));
        }

        @Test
        @DisplayName("Player move completing full board returns DRAW")
        void shouldReturnDrawWhenFull() throws Exception {
            // X O X
            // X O O
            // O X _
            List<String> board = Arrays.asList(
                "X", "O", "X",
                "X", "O", "O",
                "O", "X", ""
            );
            MoveRequest request = new MoveRequest(board, 9, Difficulty.MEDIUM);

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAW")))
                .andExpect(jsonPath("$.board[8]", is("X")))
                .andExpect(jsonPath("$.computerMove", nullValue()))
                .andExpect(jsonPath("$.nextTurn", nullValue()));
        }
    }

    @Nested
    @DisplayName("Invalid Requests & Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Rejects null board with 400 Bad Request")
        void shouldRejectNullBoard() throws Exception {
            String payload = """
                {
                    "board": null,
                    "position": 5,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_BOARD")))
                .andExpect(jsonPath("$.message", containsString("Board must not be null")));
        }

        @Test
        @DisplayName("Rejects board with fewer than 9 cells")
        void shouldRejectBoardWithFewerCells() throws Exception {
            String payload = """
                {
                    "board": ["X", "O"],
                    "position": 1,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_BOARD")))
                .andExpect(jsonPath("$.message", containsString("must contain exactly 9 cells")));
        }

        @Test
        @DisplayName("Rejects board with more than 9 cells")
        void shouldRejectBoardWithMoreCells() throws Exception {
            String payload = """
                {
                    "board": ["", "", "", "", "", "", "", "", "", ""],
                    "position": 1,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_BOARD")));
        }

        @Test
        @DisplayName("Rejects board with invalid symbols")
        void shouldRejectInvalidSymbols() throws Exception {
            String payload = """
                {
                    "board": ["X", "O", "Z", "", "", "", "", "", ""],
                    "position": 5,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_BOARD")))
                .andExpect(jsonPath("$.message", containsString("Invalid symbol 'Z'")));
        }

        @Test
        @DisplayName("Rejects position out of range (10)")
        void shouldRejectPositionOutOfRange() throws Exception {
            String payload = """
                {
                    "board": ["", "", "", "", "", "", "", "", ""],
                    "position": 10,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_MOVE")))
                .andExpect(jsonPath("$.message", containsString("Must be between 1 and 9")));
        }

        @Test
        @DisplayName("Rejects move on occupied position")
        void shouldRejectOccupiedPosition() throws Exception {
            String payload = """
                {
                    "board": ["X", "O", "", "", "", "", "", "", ""],
                    "position": 1,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_MOVE")))
                .andExpect(jsonPath("$.message", containsString("already occupied")));
        }

        @Test
        @DisplayName("Rejects move on already finished game")
        void shouldRejectMoveOnFinishedGame() throws Exception {
            // Player already won
            String payload = """
                {
                    "board": ["X", "X", "X", "O", "O", "O", "", "", ""],
                    "position": 7,
                    "difficulty": "MEDIUM"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_MOVE")))
                .andExpect(jsonPath("$.message", containsString("already won")));
        }

        @Test
        @DisplayName("Rejects invalid difficulty enum with BAD_REQUEST")
        void shouldRejectInvalidDifficulty() throws Exception {
            String payload = """
                {
                    "board": ["", "", "", "", "", "", "", "", ""],
                    "position": 5,
                    "difficulty": "IMPOSSIBLE"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
        }

        @Test
        @DisplayName("Processes HARD difficulty move successfully with 200 OK")
        void shouldProcessHardDifficultyMoveSuccessfully() throws Exception {
            String payload = """
                {
                    "board": ["", "", "", "", "", "", "", "", ""],
                    "position": 5,
                    "difficulty": "HARD"
                }
                """;

            mockMvc.perform(post("/api/game/move")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.board[4]", is("X")))
                .andExpect(jsonPath("$.computerMove", notNullValue()));
        }
    }
}
