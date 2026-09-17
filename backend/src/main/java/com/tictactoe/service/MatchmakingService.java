package com.tictactoe.service;

import com.tictactoe.model.WaitingPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory FIFO matchmaking service for pairing waiting multiplayer players.
 * Application-instance-local queue (MVP2 design limitation).
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private final ConcurrentLinkedQueue<WaitingPlayer> waitingQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, WaitingPlayer> queuedUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionToUser = new ConcurrentHashMap<>();

    public record MatchPair(WaitingPlayer player1, WaitingPlayer player2) {}

    /**
     * Enqueues a player in the matchmaking queue or pairs them with a waiting opponent.
     *
     * @param userId player ID
     * @param username player username
     * @param stompSessionId client STOMP session ID
     * @return Optional containing MatchPair if two players were matched, or empty if player is waiting
     */
    public synchronized Optional<MatchPair> enqueuePlayer(Long userId, String username, String stompSessionId) {
        return enqueuePlayer(userId, username, stompSessionId, stompSessionId);
    }

    /**
     * Enqueues a player in the matchmaking queue or pairs them with a waiting opponent.
     *
     * @param userId player ID
     * @param username player username
     * @param stompSessionId client STOMP session ID
     * @param principalName client Principal name
     * @return Optional containing MatchPair if two players were matched, or empty if player is waiting
     */
    public synchronized Optional<MatchPair> enqueuePlayer(Long userId, String username, String stompSessionId, String principalName) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null for matchmaking.");
        }

        if (queuedUsers.containsKey(userId)) {
            log.warn("User {} is already in the matchmaking queue.", userId);
            throw new IllegalStateException("ALREADY_IN_MATCHMAKING: User " + userId + " is already in matchmaking.");
        }

        WaitingPlayer newPlayer = new WaitingPlayer(userId, username, stompSessionId, principalName);

        // Find next valid waiting player in FIFO queue
        WaitingPlayer opponent = null;
        while (!waitingQueue.isEmpty()) {
            WaitingPlayer candidate = waitingQueue.poll();
            if (candidate != null && queuedUsers.containsKey(candidate.getUserId())) {
                // Ensure the candidate isn't somehow the same user
                if (!candidate.getUserId().equals(userId)) {
                    opponent = candidate;
                    break;
                }
            }
        }

        if (opponent != null) {
            // Remove opponent from active maps
            queuedUsers.remove(opponent.getUserId());
            if (opponent.getStompSessionId() != null) {
                sessionToUser.remove(opponent.getStompSessionId());
            }

            log.info("Matched players: {} ({}) and {} ({})",
                    opponent.getUsername(), opponent.getUserId(),
                    newPlayer.getUsername(), newPlayer.getUserId());

            return Optional.of(new MatchPair(opponent, newPlayer));
        }

        // No waiting opponent; enqueue this player
        queuedUsers.put(userId, newPlayer);
        if (stompSessionId != null) {
            sessionToUser.put(stompSessionId, userId);
        }
        waitingQueue.offer(newPlayer);

        log.info("Player {} ({}) joined matchmaking queue. Queue size: {}",
                username, userId, queuedUsers.size());

        return Optional.empty();
    }

    /**
     * Removes a player from the matchmaking queue.
     */
    public synchronized boolean leaveQueue(Long userId) {
        if (userId == null) {
            return false;
        }

        WaitingPlayer removed = queuedUsers.remove(userId);
        if (removed != null) {
            if (removed.getStompSessionId() != null) {
                sessionToUser.remove(removed.getStompSessionId());
            }
            log.info("Player {} ({}) left the matchmaking queue.", removed.getUsername(), userId);
            return true;
        }
        return false;
    }

    /**
     * Cleans up queue if a client disconnects while waiting.
     */
    public synchronized boolean removeBySessionId(String stompSessionId) {
        if (stompSessionId == null) {
            return false;
        }
        Long userId = sessionToUser.remove(stompSessionId);
        if (userId != null) {
            queuedUsers.remove(userId);
            log.info("Cleaned up disconnected session {} (userId: {}) from queue.", stompSessionId, userId);
            return true;
        }
        return false;
    }

    public synchronized boolean isUserInQueue(Long userId) {
        return userId != null && queuedUsers.containsKey(userId);
    }

    public synchronized int getQueueSize() {
        return queuedUsers.size();
    }

    public synchronized void clear() {
        waitingQueue.clear();
        queuedUsers.clear();
        sessionToUser.clear();
    }
}
