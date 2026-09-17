/**
 * Tic-Tac-Toe WebSocket & STOMP Client Manager
 *
 * Manages real-time communication with the Spring Boot WebSocket broker:
 * - Connection lifecycle (SockJS + STOMP)
 * - Matchmaking queue join/leave
 * - Game session move submission
 * - Real-time state event dispatching
 */

(function (window) {
    'use strict';

    class SocketManager {
        constructor() {
            this.stompClient = null;
            this.isConnected = false;
            this.isConnecting = false;
            this.gameSubscription = null;
            this.activeGameId = null;

            // Event callbacks
            this.onConnectCallbacks = [];
            this.onDisconnectCallbacks = [];
            this.onMatchFoundCallbacks = [];
            this.onGameStateCallbacks = [];
            this.onErrorCallbacks = [];
        }

        connect(apiBase = "") {
            if (this.isConnected || this.isConnecting) return;

            this.isConnecting = true;

            try {
                let socket;
                if (typeof SockJS !== "undefined") {
                    socket = new SockJS(`${apiBase}/ws`);
                } else {
                    const wsProto = window.location.protocol === "https:" ? "wss:" : "ws:";
                    const wsHost = window.location.host || "localhost:8080";
                    socket = new WebSocket(`${wsProto}//${wsHost}/ws`);
                }

                if (typeof Stomp === "undefined") {
                    console.error("STOMP library is not available.");
                    this.isConnecting = false;
                    this._triggerDisconnect();
                    return;
                }

                this.stompClient = Stomp.over(socket);
                this.stompClient.debug = null; // Suppress verbose frame logs

                this.stompClient.connect({}, 
                    frame => this._handleConnected(frame),
                    err => this._handleError(err)
                );
            } catch (err) {
                console.error("Failed to establish WebSocket connection:", err);
                this.isConnecting = false;
                this._triggerDisconnect();
            }
        }

        _handleConnected(frame) {
            this.isConnected = true;
            this.isConnecting = false;

            // Subscribe to user private match queue
            this.stompClient.subscribe("/user/queue/match", message => {
                try {
                    const match = JSON.parse(message.body);
                    this._triggerMatchFound(match);
                } catch (e) {
                    console.error("Failed to parse match payload:", e);
                }
            });

            // Subscribe to user private errors
            this.stompClient.subscribe("/user/queue/errors", message => {
                try {
                    const error = JSON.parse(message.body);
                    this._triggerError(error);
                } catch (e) {
                    console.error("Failed to parse error payload:", e);
                }
            });

            this.onConnectCallbacks.forEach(cb => cb(frame));
        }

        _handleError(err) {
            this.isConnected = false;
            this.isConnecting = false;
            this.stompClient = null;
            this._triggerDisconnect();
        }

        _triggerDisconnect() {
            this.onDisconnectCallbacks.forEach(cb => cb());
        }

        _triggerMatchFound(match) {
            this.onMatchFoundCallbacks.forEach(cb => cb(match));
        }

        _triggerGameState(state) {
            this.onGameStateCallbacks.forEach(cb => cb(state));
        }

        _triggerError(err) {
            this.onErrorCallbacks.forEach(cb => cb(err));
        }

        subscribeUserMatchFallback(userId) {
            if (!this.stompClient || !this.isConnected || !userId) return;
            this.stompClient.subscribe(`/topic/match/${userId}`, message => {
                try {
                    const match = JSON.parse(message.body);
                    this._triggerMatchFound(match);
                } catch (e) {
                    console.error("Failed to parse match payload:", e);
                }
            });
        }

        subscribeGameSession(gameId) {
            if (!this.stompClient || !this.isConnected) return;

            if (this.gameSubscription) {
                this.gameSubscription.unsubscribe();
                this.gameSubscription = null;
            }

            this.activeGameId = gameId;
            this.gameSubscription = this.stompClient.subscribe(`/topic/game/${gameId}`, message => {
                try {
                    const state = JSON.parse(message.body);
                    this._triggerGameState(state);
                } catch (e) {
                    console.error("Failed to parse game state:", e);
                }
            });
        }

        unsubscribeGameSession() {
            if (this.gameSubscription) {
                this.gameSubscription.unsubscribe();
                this.gameSubscription = null;
            }
            this.activeGameId = null;
        }

        joinMatchmaking(userId, username) {
            if (!this.isConnected || !this.stompClient) {
                throw new Error("Multiplayer server is offline.");
            }
            this.stompClient.send("/app/matchmaking/join", {}, JSON.stringify({ userId, username }));
        }

        leaveMatchmaking(userId) {
            if (!this.isConnected || !this.stompClient) return;
            this.stompClient.send("/app/matchmaking/leave", {}, JSON.stringify({ userId }));
        }

        sendMove(gameId, position, userId) {
            if (!this.isConnected || !this.stompClient) {
                throw new Error("Disconnected from game server.");
            }
            this.stompClient.send(`/app/game/${gameId}/move`, {}, JSON.stringify({ position, userId }));
        }

        onConnect(callback) { this.onConnectCallbacks.push(callback); }
        onDisconnect(callback) { this.onDisconnectCallbacks.push(callback); }
        onMatchFound(callback) { this.onMatchFoundCallbacks.push(callback); }
        onGameState(callback) { this.onGameStateCallbacks.push(callback); }
        onError(callback) { this.onErrorCallbacks.push(callback); }
    }

    window.TicTacToeSocket = new SocketManager();
})(window);
