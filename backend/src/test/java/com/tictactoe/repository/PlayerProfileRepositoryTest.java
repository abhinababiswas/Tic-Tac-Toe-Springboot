package com.tictactoe.repository;

import com.tictactoe.entity.PlayerProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class PlayerProfileRepositoryTest {

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should persist and retrieve player profile by ID and username")
    void shouldPersistAndRetrieveUser() {
        PlayerProfile profile = new PlayerProfile("alice");
        PlayerProfile saved = playerProfileRepository.save(profile);
        entityManager.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<PlayerProfile> found = playerProfileRepository.findByUsername("alice");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Should find player profile ignoring case")
    void shouldFindUsernameIgnoreCase() {
        PlayerProfile profile = new PlayerProfile("BobTheBuilder");
        playerProfileRepository.save(profile);
        entityManager.flush();

        Optional<PlayerProfile> found = playerProfileRepository.findByUsernameIgnoreCase("bobthebuilder");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("BobTheBuilder");

        assertThat(playerProfileRepository.existsByUsernameIgnoreCase("BOBTHEBUILDER")).isTrue();
        assertThat(playerProfileRepository.existsByUsernameIgnoreCase("charlie")).isFalse();
    }

    @Test
    @DisplayName("Should enforce unique username constraint")
    void shouldEnforceUniqueUsername() {
        PlayerProfile user1 = new PlayerProfile("unique_user");
        playerProfileRepository.save(user1);
        entityManager.flush();

        PlayerProfile user2 = new PlayerProfile("unique_user");
        assertThrows(DataIntegrityViolationException.class, () -> {
            playerProfileRepository.save(user2);
            entityManager.flush();
        });
    }
}
