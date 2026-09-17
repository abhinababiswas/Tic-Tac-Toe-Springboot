package com.tictactoe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactoe.entity.GameRecord;
import com.tictactoe.entity.PlayerProfile;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.MoveRequest;
import com.tictactoe.repository.GameRecordRepository;
import com.tictactoe.repository.PlayerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GamePersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    private PlayerProfile registeredUser;

    @BeforeEach
    void setUp() {
        gameRecordRepository.deleteAll();
        playerProfileRepository.deleteAll();
        registeredUser = playerProfileRepository.save(new PlayerProfile("test_player"));
    }

    @Test
    @DisplayName("Guest game completion should not persist any database records (MVP1 compatibility)")
    void guestGameShouldNotPersist() throws Exception {
        // Winning move for X: row 1 (positions 1, 2, 3)
        List<String> board = Arrays.asList(
            "X", "X", "",
            "O", "O", "",
            "", "", ""
        );

        MoveRequest request = new MoveRequest(board, 3, Difficulty.EASY, null, Instant.now());

        mockMvc.perform(post("/api/game/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("PLAYER_WON")));

        assertThat(gameRecordRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Registered user game completion should persist game record, appear in history, and reflect on leaderboard")
    void registeredUserGameShouldPersistAndShowInHistoryAndLeaderboard() throws Exception {
        // Winning move for X at position 3
        List<String> board = Arrays.asList(
            "X", "X", "",
            "O", "O", "",
            "", "", ""
        );

        MoveRequest request = new MoveRequest(
            board, 3, Difficulty.HARD, registeredUser.getId(), Instant.now().minusSeconds(15)
        );

        // 1. Execute winning move
        mockMvc.perform(post("/api/game/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("PLAYER_WON")));

        // 2. Verify database contains 1 record
        List<GameRecord> records = gameRecordRepository.findAll();
        assertThat(records).hasSize(1);
        GameRecord record = records.get(0);
        assertThat(record.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(record.getStatus()).isEqualTo(com.tictactoe.model.GameStatus.PLAYER_WON);

        // 3. Verify history endpoint returns the persisted game
        mockMvc.perform(get("/api/users/" + registeredUser.getId() + "/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", is(1)))
            .andExpect(jsonPath("$.content[0].outcome", is("WIN")))
            .andExpect(jsonPath("$.content[0].difficulty", is("HARD")))
            .andExpect(jsonPath("$.content[0].opponent", is("Computer")));

        // 4. Verify leaderboard endpoint includes user with 1 win and 100% win rate
        mockMvc.perform(get("/api/leaderboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username", is("test_player")))
            .andExpect(jsonPath("$[0].wins", is(1)))
            .andExpect(jsonPath("$[0].gamesPlayed", is(1)))
            .andExpect(jsonPath("$[0].winRate", is(100.0)));
    }
}
