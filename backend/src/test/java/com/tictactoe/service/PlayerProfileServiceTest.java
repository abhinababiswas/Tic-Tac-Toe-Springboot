package com.tictactoe.service;

import com.tictactoe.dto.CreateUserRequest;
import com.tictactoe.dto.UserProfileResponse;
import com.tictactoe.entity.PlayerProfile;
import com.tictactoe.exception.ResourceNotFoundException;
import com.tictactoe.exception.UsernameAlreadyExistsException;
import com.tictactoe.repository.PlayerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerProfileServiceTest {

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    private PlayerProfileService playerProfileService;

    @BeforeEach
    void setUp() {
        playerProfileService = new PlayerProfileService(playerProfileRepository);
    }

    @Test
    @DisplayName("Should create player profile when username is valid and available")
    void shouldCreateProfile() {
        CreateUserRequest request = new CreateUserRequest("player_one");

        when(playerProfileRepository.existsByUsernameIgnoreCase("player_one")).thenReturn(false);
        PlayerProfile savedEntity = new PlayerProfile("player_one");
        savedEntity.setId(1L);
        savedEntity.setCreatedAt(Instant.now());
        savedEntity.setUpdatedAt(Instant.now());
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenReturn(savedEntity);

        UserProfileResponse response = playerProfileService.createProfile(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("player_one");
        verify(playerProfileRepository).save(any(PlayerProfile.class));
    }

    @Test
    @DisplayName("Should throw UsernameAlreadyExistsException when username is already taken")
    void shouldThrowWhenUsernameTaken() {
        CreateUserRequest request = new CreateUserRequest("existing_user");
        when(playerProfileRepository.existsByUsernameIgnoreCase("existing_user")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> playerProfileService.createProfile(request));
        verify(playerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject invalid username formats")
    void shouldRejectInvalidUsernames() {
        // Too short (< 3 chars)
        assertThrows(IllegalArgumentException.class, () -> playerProfileService.createProfile(new CreateUserRequest("ab")));

        // Too long (> 30 chars)
        assertThrows(IllegalArgumentException.class, () -> playerProfileService.createProfile(new CreateUserRequest("a".repeat(31))));

        // Invalid characters (spaces, symbols)
        assertThrows(IllegalArgumentException.class, () -> playerProfileService.createProfile(new CreateUserRequest("user name")));
        assertThrows(IllegalArgumentException.class, () -> playerProfileService.createProfile(new CreateUserRequest("user@name!")));

        // Null
        assertThrows(IllegalArgumentException.class, () -> playerProfileService.createProfile(new CreateUserRequest(null)));
    }

    @Test
    @DisplayName("Should retrieve profile by ID or throw ResourceNotFoundException")
    void shouldGetProfileById() {
        PlayerProfile profile = new PlayerProfile("alice");
        profile.setId(42L);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        when(playerProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        when(playerProfileRepository.findById(99L)).thenReturn(Optional.empty());

        UserProfileResponse found = playerProfileService.getProfileById(42L);
        assertThat(found.getUsername()).isEqualTo("alice");

        assertThrows(ResourceNotFoundException.class, () -> playerProfileService.getProfileById(99L));
    }

    @Test
    @DisplayName("Should retrieve profile by username or throw ResourceNotFoundException")
    void shouldGetProfileByUsername() {
        PlayerProfile profile = new PlayerProfile("bob");
        profile.setId(10L);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        when(playerProfileRepository.findByUsernameIgnoreCase("bob")).thenReturn(Optional.of(profile));
        when(playerProfileRepository.findByUsernameIgnoreCase("charlie")).thenReturn(Optional.empty());

        UserProfileResponse found = playerProfileService.getProfileByUsername("bob");
        assertThat(found.getId()).isEqualTo(10L);

        assertThrows(ResourceNotFoundException.class, () -> playerProfileService.getProfileByUsername("charlie"));
    }
}
