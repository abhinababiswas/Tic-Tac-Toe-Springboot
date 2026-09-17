package com.tictactoe.service;

import com.tictactoe.dto.CreateUserRequest;
import com.tictactoe.dto.UserProfileResponse;
import com.tictactoe.entity.PlayerProfile;
import com.tictactoe.exception.ResourceNotFoundException;
import com.tictactoe.exception.UsernameAlreadyExistsException;
import com.tictactoe.repository.PlayerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Service managing user/player profile registration, retrieval, and validation.
 */
@Service
public class PlayerProfileService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private final PlayerProfileRepository playerProfileRepository;

    @Autowired
    public PlayerProfileService(PlayerProfileRepository playerProfileRepository) {
        this.playerProfileRepository = playerProfileRepository;
    }

    @Transactional
    public UserProfileResponse createProfile(CreateUserRequest request) {
        if (request == null || request.getUsername() == null) {
            throw new IllegalArgumentException("Username must not be null.");
        }

        String trimmed = request.getUsername().trim();
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Username must be between 3 and 30 characters and contain only letters, numbers, and underscores.");
        }

        if (playerProfileRepository.existsByUsernameIgnoreCase(trimmed)) {
            throw new UsernameAlreadyExistsException("Username '" + trimmed + "' is already taken.");
        }

        PlayerProfile profile = new PlayerProfile(trimmed);
        PlayerProfile saved = playerProfileRepository.save(profile);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID must not be null.");
        }
        PlayerProfile profile = playerProfileRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        PlayerProfile profile = playerProfileRepository.findByUsernameIgnoreCase(username.trim())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username.trim()));
        return toResponse(profile);
    }

    private UserProfileResponse toResponse(PlayerProfile entity) {
        return new UserProfileResponse(
            entity.getId(),
            entity.getUsername(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
