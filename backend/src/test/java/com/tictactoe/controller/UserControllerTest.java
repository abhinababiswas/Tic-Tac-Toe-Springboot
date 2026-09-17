package com.tictactoe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactoe.dto.CreateUserRequest;
import com.tictactoe.entity.GameParticipant;
import com.tictactoe.entity.GameRecord;
import com.tictactoe.entity.GameResult;
import com.tictactoe.entity.ParticipantOutcome;
import com.tictactoe.entity.ParticipantType;
import com.tictactoe.entity.PlayerProfile;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.GameMode;
import com.tictactoe.model.GameStatus;
import com.tictactoe.model.PlayerSymbol;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @BeforeEach
    void setUp() {
        gameRecordRepository.deleteAll();
        playerProfileRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/users - Should register user and return 201 Created")
    void shouldRegisterUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("neo");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username", is("neo")))
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("POST /api/users - Should return 409 Conflict when username is taken")
    void shouldReturnConflictForDuplicateUsername() throws Exception {
        playerProfileRepository.save(new PlayerProfile("trinity"));

        CreateUserRequest request = new CreateUserRequest("trinity");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error", is("USERNAME_ALREADY_EXISTS")))
            .andExpect(jsonPath("$.message", is("Username 'trinity' is already taken.")));
    }

    @Test
    @DisplayName("POST /api/users - Should return 400 Bad Request for invalid username format")
    void shouldReturnBadRequestForInvalidFormat() throws Exception {
        CreateUserRequest request = new CreateUserRequest("no"); // too short (<3 chars)

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("INVALID_ARGUMENT")));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return 200 OK when user exists")
    void shouldGetUserById() throws Exception {
        PlayerProfile saved = playerProfileRepository.save(new PlayerProfile("morpheus"));

        mockMvc.perform(get("/api/users/" + saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
            .andExpect(jsonPath("$.username", is("morpheus")));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return 404 Not Found when user does not exist")
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/users/{id}/history - Should return 200 OK with paginated game history")
    void shouldGetUserHistory() throws Exception {
        PlayerProfile user = playerProfileRepository.save(new PlayerProfile("gamer_one"));

        GameRecord record = new GameRecord(
            GameMode.COMPUTER, Difficulty.HARD, GameStatus.PLAYER_WON, GameResult.PLAYER_WIN,
            Instant.now().minusSeconds(60), Instant.now()
        );
        record.addParticipant(new GameParticipant(record, user, PlayerSymbol.X, ParticipantType.HUMAN, ParticipantOutcome.WIN));
        record.addParticipant(new GameParticipant(record, null, PlayerSymbol.O, ParticipantType.COMPUTER, ParticipantOutcome.LOSS));
        gameRecordRepository.save(record);

        mockMvc.perform(get("/api/users/" + user.getId() + "/history?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].outcome", is("WIN")))
            .andExpect(jsonPath("$.content[0].difficulty", is("HARD")))
            .andExpect(jsonPath("$.content[0].opponent", is("Computer")))
            .andExpect(jsonPath("$.totalElements", is(1)));
    }
}
