package com.tictactoe.controller;

import com.tictactoe.model.GameResponse;
import com.tictactoe.model.MoveRequest;
import com.tictactoe.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller handling HTTP requests for Tic-Tac-Toe gameplay.
 * Thin HTTP boundary delegating all domain logic and orchestration to GameService.
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5500",
    "http://127.0.0.1:5500",
    "http://localhost:8080",
    "http://127.0.0.1:8080"
})
public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Primary game turn endpoint:
     * Receives player's move, current board, and difficulty level;
     * returns authoritative updated board and game status.
     */
    @PostMapping("/move")
    public ResponseEntity<GameResponse> makeMove(@RequestBody MoveRequest request) {
        GameResponse response = gameService.processMove(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Optional health endpoint for connectivity checks.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "tictactoe-backend"
        ));
    }
}
