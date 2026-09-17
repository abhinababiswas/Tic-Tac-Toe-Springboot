/**
 * Tic-Tac-Toe REST API Client
 *
 * Handles all HTTP communication with the Spring Boot backend:
 * - Computer moves (/api/game/move)
 * - User profile creation & lookup (/api/users)
 * - Leaderboards (/api/leaderboard)
 * - Personal game history (/api/users/{id}/history)
 */

(function (window) {
    'use strict';

    // Auto-detect backend port/host
    const isCloudOrSpringBoot = window.location.protocol.startsWith("http") && (
        window.location.port === "8080" || 
        (window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1")
    );
    const API_BASE = isCloudOrSpringBoot ? "" : "http://localhost:8080";

    const Api = {
        baseUrl: API_BASE,

        /**
         * Submit a player move in Player vs Computer mode.
         * @param {Object} data { board, move, difficulty, userId, startedAt }
         * @returns {Promise<Object>} { board, status, winner, winningLine }
         */
        async sendComputerMove(data) {
            const res = await fetch(`${API_BASE}/api/game/move`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(data)
            });

            if (!res.ok) {
                let errorMsg = "Unable to process move.";
                try {
                    const err = await res.json();
                    if (err && err.message) errorMsg = err.message;
                } catch (_) {}
                throw new Error(errorMsg);
            }
            return await res.json();
        },

        /**
         * Create a new user profile.
         * @param {string} username
         * @returns {Promise<Object>} User entity or throws
         */
        async createUser(username) {
            const res = await fetch(`${API_BASE}/api/users`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username })
            });

            if (res.status === 201) {
                return await res.json();
            }

            if (res.status === 409) {
                // Username exists, attempt retrieval
                return await this.getUserByUsername(username);
            }

            let msg = "Failed to create user.";
            try {
                const err = await res.json();
                if (err && err.message) msg = err.message;
            } catch (_) {}
            throw new Error(msg);
        },

        /**
         * Look up a user profile by username.
         * @param {string} username
         * @returns {Promise<Object>}
         */
        async getUserByUsername(username) {
            const res = await fetch(`${API_BASE}/api/users/by-username/${encodeURIComponent(username)}`);
            if (!res.ok) {
                throw new Error("User not found.");
            }
            return await res.json();
        },

        /**
         * Fetch global leaderboard rankings.
         * @param {number} limit
         * @returns {Promise<Array>}
         */
        async fetchLeaderboard(limit = 10) {
            const res = await fetch(`${API_BASE}/api/leaderboard?limit=${limit}`);
            if (!res.ok) {
                throw new Error("Failed to load leaderboard.");
            }
            return await res.json();
        },

        /**
         * Fetch paginated game history for a specific user.
         * @param {number} userId
         * @param {number} page
         * @param {number} size
         * @returns {Promise<Object>} Page object with content, totalElements, totalPages
         */
        async fetchUserHistory(userId, page = 0, size = 10) {
            const res = await fetch(`${API_BASE}/api/users/${userId}/history?page=${page}&size=${size}`);
            if (!res.ok) {
                throw new Error("Failed to load history.");
            }
            return await res.json();
        }
    };

    window.TicTacToeApi = Api;
})(window);
