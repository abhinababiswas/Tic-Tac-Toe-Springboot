package com.tictactoe.repository;

import com.tictactoe.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    Optional<PlayerProfile> findByUsername(String username);

    Optional<PlayerProfile> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
