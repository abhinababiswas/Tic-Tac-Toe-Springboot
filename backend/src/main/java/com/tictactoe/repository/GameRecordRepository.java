package com.tictactoe.repository;

import com.tictactoe.entity.GameRecord;
import com.tictactoe.model.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

    @Query("SELECT g FROM GameRecord g JOIN g.participants p WHERE p.player.id = :playerId ORDER BY g.completedAt DESC")
    Page<GameRecord> findByPlayerId(@Param("playerId") Long playerId, Pageable pageable);

    long countByStatus(GameStatus status);
}
