package com.tictactoe.service;

import com.tictactoe.model.WaitingPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MatchmakingServiceTest {

    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        matchmakingService = new MatchmakingService();
    }

    @Test
    @DisplayName("Single player joining queue waits and is not paired")
    void testSinglePlayerWaits() {
        Optional<MatchmakingService.MatchPair> result = matchmakingService.enqueuePlayer(1L, "Alice", "sess-1");

        assertTrue(result.isEmpty(), "First player should wait in the queue");
        assertEquals(1, matchmakingService.getQueueSize());
        assertTrue(matchmakingService.isUserInQueue(1L));
    }

    @Test
    @DisplayName("Second player causes immediate pairing in FIFO order")
    void testTwoPlayersMatchedFIFO() {
        Optional<MatchmakingService.MatchPair> res1 = matchmakingService.enqueuePlayer(1L, "Alice", "sess-1");
        assertTrue(res1.isEmpty());

        Optional<MatchmakingService.MatchPair> res2 = matchmakingService.enqueuePlayer(2L, "Bob", "sess-2");
        assertTrue(res2.isPresent(), "Second player should trigger a match");

        MatchmakingService.MatchPair pair = res2.get();
        assertEquals(1L, pair.player1().getUserId(), "First queued player should be player1 (FIFO)");
        assertEquals(2L, pair.player2().getUserId(), "Second queued player should be player2");

        assertEquals(0, matchmakingService.getQueueSize(), "Both players must be removed from queue upon matching");
        assertFalse(matchmakingService.isUserInQueue(1L));
        assertFalse(matchmakingService.isUserInQueue(2L));
    }

    @Test
    @DisplayName("Same player cannot join the matchmaking queue multiple times")
    void testDuplicatePlayerRejected() {
        matchmakingService.enqueuePlayer(1L, "Alice", "sess-1");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchmakingService.enqueuePlayer(1L, "Alice", "sess-1-new")
        );

        assertTrue(exception.getMessage().contains("ALREADY_IN_MATCHMAKING"));
        assertEquals(1, matchmakingService.getQueueSize());
    }

    @Test
    @DisplayName("Player can cleanly cancel and leave matchmaking queue")
    void testPlayerCanLeaveQueue() {
        matchmakingService.enqueuePlayer(1L, "Alice", "sess-1");
        assertEquals(1, matchmakingService.getQueueSize());

        boolean left = matchmakingService.leaveQueue(1L);
        assertTrue(left);
        assertEquals(0, matchmakingService.getQueueSize());
        assertFalse(matchmakingService.isUserInQueue(1L));

        // Attempting to leave again returns false
        assertFalse(matchmakingService.leaveQueue(1L));
    }

    @Test
    @DisplayName("Disconnecting session removes player from queue")
    void testDisconnectRemovesPlayerFromQueue() {
        matchmakingService.enqueuePlayer(1L, "Alice", "sess-1");
        assertEquals(1, matchmakingService.getQueueSize());

        boolean removed = matchmakingService.removeBySessionId("sess-1");
        assertTrue(removed);
        assertEquals(0, matchmakingService.getQueueSize());
        assertFalse(matchmakingService.isUserInQueue(1L));
    }

    @Test
    @DisplayName("Multiple waiting players are paired in correct successive pairs")
    void testMultipleWaitingPlayersPairedInPairs() {
        matchmakingService.enqueuePlayer(1L, "User1", "s1");
        matchmakingService.enqueuePlayer(2L, "User2", "s2"); // Pair 1-2 formed
        matchmakingService.enqueuePlayer(3L, "User3", "s3"); // Waits
        Optional<MatchmakingService.MatchPair> pair2 = matchmakingService.enqueuePlayer(4L, "User4", "s4"); // Pair 3-4 formed

        assertTrue(pair2.isPresent());
        assertEquals(3L, pair2.get().player1().getUserId());
        assertEquals(4L, pair2.get().player2().getUserId());
        assertEquals(0, matchmakingService.getQueueSize());
    }

    @Test
    @DisplayName("Null user ID throws IllegalArgumentException")
    void testNullUserIdRejected() {
        assertThrows(IllegalArgumentException.class, () -> matchmakingService.enqueuePlayer(null, "Alice", "sess"));
    }
}
