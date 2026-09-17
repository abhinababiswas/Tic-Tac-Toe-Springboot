package com.tictactoe.service;

import com.tictactoe.engine.GameEngine;
import com.tictactoe.exception.InvalidMoveException;
import com.tictactoe.model.MultiplayerGameSession;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.model.WaitingPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerConcurrencyTest {

    private GameEngine gameEngine;
    private WaitingPlayer playerX;
    private WaitingPlayer playerO;
    private MultiplayerGameSession session;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
        playerX = new WaitingPlayer(100L, "PlayerX", "sess-x");
        playerO = new WaitingPlayer(200L, "PlayerO", "sess-o");
        session = new MultiplayerGameSession("game-concurrency", playerX, playerO);
    }

    @Test
    @DisplayName("Simultaneous moves on the same cell: exactly one succeeds and second is rejected")
    void testConcurrentMovesOnSameCell() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    session.applyMove(100L, 5, gameEngine); // Center
                    successCount.incrementAndGet();
                } catch (InvalidMoveException e) {
                    errorCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();
        assertTrue(doneGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one move must succeed");
        assertEquals(threadCount - 1, errorCount.get(), "All conflicting concurrent moves must fail");
        assertEquals("X", session.getBoard().getCell(4), "Cell 5 must contain X");
        assertEquals(PlayerSymbol.O, session.getCurrentTurn(), "Turn must advance to O exactly once");
    }

    @Test
    @DisplayName("Simultaneous moves by same player on different positions: only one move accepted for that turn")
    void testConcurrentMovesDifferentPositionsSamePlayer() throws InterruptedException {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        // All threads attempt different valid positions (1 through 8) simultaneously as Player X
        for (int i = 1; i <= threadCount; i++) {
            final int pos = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    session.applyMove(100L, pos, gameEngine);
                    successCount.incrementAndGet();
                } catch (InvalidMoveException e) {
                    rejectedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(doneGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one move can be accepted for Player X's turn");
        assertEquals(threadCount - 1, rejectedCount.get(), "All other simultaneous attempts must be rejected");
        assertEquals(PlayerSymbol.O, session.getCurrentTurn(), "Turn must switch to O");
        assertEquals(1, session.getBoard().countSymbol("X"), "Board must contain exactly 1 X");
    }

    @Test
    @DisplayName("Out-of-turn move attempt is immediately rejected without altering board")
    void testOutOfTurnMoveRejected() {
        assertThrows(
                InvalidMoveException.class,
                () -> session.applyMove(200L, 1, gameEngine),
                "Player O cannot move during Player X's turn"
        );

        assertEquals(PlayerSymbol.X, session.getCurrentTurn());
        assertEquals("", session.getBoard().getCell(0));
    }
}
