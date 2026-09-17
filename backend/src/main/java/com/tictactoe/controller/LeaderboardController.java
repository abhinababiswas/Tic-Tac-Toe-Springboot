package com.tictactoe.controller;

import com.tictactoe.dto.LeaderboardEntryResponse;
import com.tictactoe.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller providing leaderboard rankings and competitive statistics.
 */
@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5500",
    "http://127.0.0.1:5500",
    "http://localhost:8080",
    "http://127.0.0.1:8080"
})
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @Autowired
    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Retrieves ranked player leaderboard based on completed game records.
     */
    @GetMapping
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(
            @RequestParam(defaultValue = "20") int limit) {
        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }
}
