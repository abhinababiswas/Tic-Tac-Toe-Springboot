package com.tictactoe.controller;

import com.tictactoe.dto.websocket.GameStateMessage;
import com.tictactoe.dto.websocket.JoinMatchmakingRequest;
import com.tictactoe.dto.websocket.MatchFoundMessage;
import com.tictactoe.dto.websocket.MultiplayerMoveRequest;
import com.tictactoe.model.PlayerSymbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultiplayerWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    @DisplayName("Two players connect via STOMP, join matchmaking, get paired, and exchange a move")
    void testWebSocketMatchmakingAndGameplay() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";

        CompletableFuture<MatchFoundMessage> matchFuture1 = new CompletableFuture<>();
        CompletableFuture<MatchFoundMessage> matchFuture2 = new CompletableFuture<>();

        // Connect Client 1 (Alice)
        StompSession session1 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        assertTrue(session1.isConnected());

        // Connect Client 2 (Bob)
        StompSession session2 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        assertTrue(session2.isConnected());

        // Subscribe to user match queue
        session1.subscribe("/user/queue/match", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MatchFoundMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                matchFuture1.complete((MatchFoundMessage) payload);
            }
        });

        session2.subscribe("/user/queue/match", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MatchFoundMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                matchFuture2.complete((MatchFoundMessage) payload);
            }
        });

        // Also subscribe to topic fallback to ensure receipt regardless of principal routing in tests
        session1.subscribe("/topic/match/1001", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MatchFoundMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                matchFuture1.complete((MatchFoundMessage) payload);
            }
        });

        session2.subscribe("/topic/match/1002", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MatchFoundMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                matchFuture2.complete((MatchFoundMessage) payload);
            }
        });

        // Give subscription frames a brief moment to register
        Thread.sleep(200);

        // Join matchmaking
        session1.send("/app/matchmaking/join", new JoinMatchmakingRequest(1001L, "Alice"));
        session2.send("/app/matchmaking/join", new JoinMatchmakingRequest(1002L, "Bob"));

        // Await match creation
        MatchFoundMessage match1 = matchFuture1.get(10, TimeUnit.SECONDS);
        MatchFoundMessage match2 = matchFuture2.get(10, TimeUnit.SECONDS);

        assertNotNull(match1);
        assertNotNull(match2);
        assertEquals(match1.getGameId(), match2.getGameId());
        assertNotEquals(match1.getYourSymbol(), match2.getYourSymbol());

        String gameId = match1.getGameId();

        // Subscribe to game topic
        CompletableFuture<GameStateMessage> moveFuture = new CompletableFuture<>();
        session1.subscribe("/topic/game/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return GameStateMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                GameStateMessage msg = (GameStateMessage) payload;
                if ("GAME_UPDATE".equals(msg.getType())) {
                    moveFuture.complete(msg);
                }
            }
        });

        Thread.sleep(200);

        // Identify who has 'X' to make the first move
        Long playerXId = (match1.getYourSymbol() == PlayerSymbol.X) ? 1001L : 1002L;
        StompSession sessionX = (match1.getYourSymbol() == PlayerSymbol.X) ? session1 : session2;

        sessionX.send("/app/game/" + gameId + "/move", new MultiplayerMoveRequest(5, playerXId));

        GameStateMessage updatedState = moveFuture.get(10, TimeUnit.SECONDS);
        assertNotNull(updatedState);
        assertEquals(5, updatedState.getLastMovePosition());
        assertEquals(PlayerSymbol.X, updatedState.getLastMovePlayer());
        assertEquals(PlayerSymbol.O, updatedState.getCurrentTurn());

        session1.disconnect();
        session2.disconnect();
    }
}
