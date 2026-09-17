package com.tictactoe.controller;

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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("GET /api/leaderboard - Should return 200 OK with ranked leaderboard entries from persistence")
    void shouldReturnLeaderboard() throws Exception {
        PlayerProfile userA = playerProfileRepository.save(new PlayerProfile("Alpha"));
        PlayerProfile userB = playerProfileRepository.save(new PlayerProfile("Beta"));

        Instant now = Instant.now();

        // Alpha: 2 wins
        for (int i = 0; i < 2; i++) {
            GameRecord g = new GameRecord(GameMode.COMPUTER, Difficulty.HARD, GameStatus.PLAYER_WON, GameResult.PLAYER_WIN, now, now);
            g.addParticipant(new GameParticipant(g, userA, PlayerSymbol.X, ParticipantType.HUMAN, ParticipantOutcome.WIN));
            g.addParticipant(new GameParticipant(g, null, PlayerSymbol.O, ParticipantType.COMPUTER, ParticipantOutcome.LOSS));
            gameRecordRepository.save(g);
        }

        // Beta: 1 win
        GameRecord gBeta = new GameRecord(GameMode.COMPUTER, Difficulty.MEDIUM, GameStatus.PLAYER_WON, GameResult.PLAYER_WIN, now, now);
        gBeta.addParticipant(new GameParticipant(gBeta, userB, PlayerSymbol.X, ParticipantType.HUMAN, ParticipantOutcome.WIN));
        gBeta.addParticipant(new GameParticipant(gBeta, null, PlayerSymbol.O, ParticipantType.COMPUTER, ParticipantOutcome.LOSS));
        gameRecordRepository.save(gBeta);

        mockMvc.perform(get("/api/leaderboard?limit=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].rank", is(1)))
            .andExpect(jsonPath("$[0].username", is("Alpha")))
            .andExpect(jsonPath("$[0].wins", is(2)))
            .andExpect(jsonPath("$[0].winRate", is(100.0)))
            .andExpect(jsonPath("$[1].rank", is(2)))
            .andExpect(jsonPath("$[1].username", is("Beta")))
            .andExpect(jsonPath("$[1].wins", is(1)));
    }
}
