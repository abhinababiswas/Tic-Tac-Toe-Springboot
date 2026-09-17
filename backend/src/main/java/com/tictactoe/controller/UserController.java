package com.tictactoe.controller;

import com.tictactoe.dto.CreateUserRequest;
import com.tictactoe.dto.GameHistoryResponse;
import com.tictactoe.dto.UserProfileResponse;
import com.tictactoe.service.GameHistoryService;
import com.tictactoe.service.PlayerProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing player profiles and user game history.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5500",
    "http://127.0.0.1:5500",
    "http://localhost:8080",
    "http://127.0.0.1:8080"
})
public class UserController {

    private final PlayerProfileService playerProfileService;
    private final GameHistoryService gameHistoryService;

    @Autowired
    public UserController(PlayerProfileService playerProfileService, GameHistoryService gameHistoryService) {
        this.playerProfileService = playerProfileService;
        this.gameHistoryService = gameHistoryService;
    }

    /**
     * Registers a new player profile.
     */
    @PostMapping
    public ResponseEntity<UserProfileResponse> createUser(@RequestBody CreateUserRequest request) {
        UserProfileResponse response = playerProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves player profile by unique ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        UserProfileResponse response = playerProfileService.getProfileById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves player profile by username.
     */
    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserProfileResponse> getUserByUsername(@PathVariable String username) {
        UserProfileResponse response = playerProfileService.getProfileByUsername(username);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated game history for a registered user.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<Page<GameHistoryResponse>> getUserHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 100));
        int boundedPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);
        Page<GameHistoryResponse> history = gameHistoryService.getUserHistory(id, pageable);
        return ResponseEntity.ok(history);
    }
}
