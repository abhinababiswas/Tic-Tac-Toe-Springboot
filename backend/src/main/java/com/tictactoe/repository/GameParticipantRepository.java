package com.tictactoe.repository;

import com.tictactoe.entity.GameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameParticipantRepository extends JpaRepository<GameParticipant, Long> {

    List<GameParticipant> findByPlayerId(Long playerId);

    @Query("""
        SELECT u.id AS playerId,
               u.username AS username,
               COUNT(p.id) AS gamesPlayed,
               COALESCE(SUM(CASE WHEN p.outcome = com.tictactoe.entity.ParticipantOutcome.WIN THEN 1L ELSE 0L END), 0L) AS wins,
               COALESCE(SUM(CASE WHEN p.outcome = com.tictactoe.entity.ParticipantOutcome.LOSS THEN 1L ELSE 0L END), 0L) AS losses,
               COALESCE(SUM(CASE WHEN p.outcome = com.tictactoe.entity.ParticipantOutcome.DRAW THEN 1L ELSE 0L END), 0L) AS draws
        FROM PlayerProfile u
        LEFT JOIN GameParticipant p ON p.player = u
        GROUP BY u.id, u.username
    """)
    List<PlayerStatProjection> aggregatePlayerStats();
}
