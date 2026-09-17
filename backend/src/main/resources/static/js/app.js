/**
 * Tic-Tac-Toe Modern Website Application (MVP 2 - Phase 5)
 *
 * Architecture & Features:
 * - Hash-based View Routing (Home, Play, Multiplayer, Leaderboard, History).
 * - Player vs Computer (Stateless REST API with Easy, Medium, and Unbeatable Minimax AI).
 * - Online Multiplayer (WebSocket + STOMP real-time duplex channel with matchmaking queue).
 * - User Profile & Identity management with localStorage persistence.
 * - Global Leaderboard & Personal Match History.
 * - Server-Authoritative Engine: 100% of moves, rules, and outcomes validated by Spring Boot.
 */

(function (window, document) {
    'use strict';

    const INITIAL_BOARD = ["", "", "", "", "", "", "", "", ""];

    // ==========================================
    // APPLICATION STATE
    // ==========================================
    const state = {
        currentView: "home",
        user: {
            id: localStorage.getItem("tictactoe_user_id") ? parseInt(localStorage.getItem("tictactoe_user_id"), 10) : null,
            username: localStorage.getItem("tictactoe_username") || null
        },
        computerGame: {
            board: [...INITIAL_BOARD],
            difficulty: "EASY",
            status: "IN_PROGRESS",
            isRequestPending: false,
            errorMessage: null,
            startedAt: null,
            winningLine: null,
            scores: {
                player: 0,
                computer: 0,
                draws: 0
            }
        },
        multiplayer: {
            isQueueing: false,
            inMatch: false,
            gameId: null,
            yourSymbol: null,
            opponentUsername: null,
            currentTurn: null,
            board: [...INITIAL_BOARD],
            status: "IN_PROGRESS",
            winningLine: null,
            errorMessage: null
        },
        history: {
            currentPage: 0,
            pageSize: 5,
            totalPages: 1
        }
    };

    // ==========================================
    // DOM CACHE
    // ==========================================
    const dom = {
        navLinks: document.querySelectorAll(".nav-link"),
        mobileToggle: document.getElementById("mobile-toggle"),
        navLinksContainer: document.getElementById("nav-links"),
        views: document.querySelectorAll(".view-section"),
        navUsername: document.getElementById("nav-username"),
        userAvatar: document.getElementById("user-avatar"),
        btnProfile: document.getElementById("btn-profile"),

        // Profile Modal
        modalProfile: document.getElementById("profile-modal"),
        modalClose: document.getElementById("btn-modal-close"),
        modalUsernameInput: document.getElementById("modal-username-input"),
        modalSaveBtn: document.getElementById("btn-modal-save"),
        modalGuestBtn: document.getElementById("btn-modal-guest"),
        modalStatusMsg: document.getElementById("modal-status-msg"),
        modalProfileStats: document.getElementById("modal-profile-stats"),
        modalProfileId: document.getElementById("modal-profile-id"),

        // Home CTAs
        btnHeroPlay: document.getElementById("btn-hero-play"),
        btnHeroMultiplayer: document.getElementById("btn-hero-multiplayer"),
        btnHeroLeaderboard: document.getElementById("btn-hero-leaderboard"),
        btnCardComputer: document.getElementById("btn-card-computer"),
        btnCardMultiplayer: document.getElementById("btn-card-multiplayer"),

        // Play vs Computer
        diffButtons: [
            document.getElementById("diff-easy"),
            document.getElementById("diff-medium"),
            document.getElementById("diff-hard")
        ],
        diffTooltip: document.getElementById("diff-tooltip"),
        scorePlayer: document.getElementById("score-player"),
        scoreComputer: document.getElementById("score-computer"),
        scoreDraws: document.getElementById("score-draws"),
        computerStatus: document.getElementById("game-status-computer"),
        computerBoard: document.getElementById("board-computer"),
        computerCells: document.querySelectorAll("#board-computer .cell"),
        btnRestartComputer: document.getElementById("btn-restart-computer"),
        btnResetScores: document.getElementById("btn-reset-scores"),

        // Online Multiplayer
        mpConnectionBadge: document.getElementById("mp-connection-badge"),
        mpSpinner: document.getElementById("mp-spinner"),
        mpQueueMsg: document.getElementById("mp-queue-message"),
        btnMpFind: document.getElementById("btn-mp-find"),
        btnMpCancel: document.getElementById("btn-mp-cancel"),
        btnMpPlayAgain: document.getElementById("btn-mp-play-again"),
        btnMpLeave: document.getElementById("btn-mp-leave"),
        mpMatchStrip: document.getElementById("mp-match-strip"),
        mpYouSymbol: document.getElementById("mp-you-symbol"),
        mpOppName: document.getElementById("mp-opp-name"),
        mpStatus: document.getElementById("game-status-multiplayer"),
        mpBoard: document.getElementById("board-multiplayer"),
        mpCells: document.querySelectorAll("#board-multiplayer .cell"),

        // Leaderboard
        statTotalPlayers: document.getElementById("stat-total-players"),
        statTotalMatches: document.getElementById("stat-total-matches"),
        statTotalWins: document.getElementById("stat-total-wins"),
        leaderboardTableBody: document.getElementById("leaderboard-table-body"),
        btnRefreshLeaderboard: document.getElementById("btn-refresh-leaderboard"),

        // History
        historyFeed: document.getElementById("history-feed"),
        historyPagination: document.getElementById("history-pagination"),
        btnHistoryPrev: document.getElementById("btn-history-prev"),
        btnHistoryNext: document.getElementById("btn-history-next"),
        historyPageInfo: document.getElementById("history-page-info")
    };

    // ==========================================
    // INITIALIZATION & ROUTING
    // ==========================================
    function init() {
        bindEvents();
        initSocketListeners();
        updateUserProfileDisplay();

        // Handle initial URL hash route
        const initialView = window.location.hash.replace("#", "") || "home";
        navigateTo(initialView);

        // Auto-connect socket in background
        if (window.TicTacToeSocket) {
            window.TicTacToeSocket.connect(window.TicTacToeApi.baseUrl);
        }
    }

    function bindEvents() {
        // Window hash change navigation
        window.addEventListener("hashchange", () => {
            const view = window.location.hash.replace("#", "") || "home";
            navigateTo(view);
        });

        // Mobile Nav Toggle
        if (dom.mobileToggle) {
            dom.mobileToggle.addEventListener("click", () => {
                const isOpen = dom.navLinksContainer.classList.toggle("open");
                dom.mobileToggle.setAttribute("aria-expanded", isOpen ? "true" : "false");
            });
        }

        // Close mobile nav on link click
        dom.navLinks.forEach(link => {
            link.addEventListener("click", () => {
                dom.navLinksContainer.classList.remove("open");
            });
        });

        // Profile Modal
        if (dom.btnProfile) dom.btnProfile.addEventListener("click", openProfileModal);
        if (dom.modalClose) dom.modalClose.addEventListener("click", closeProfileModal);
        if (dom.modalSaveBtn) dom.modalSaveBtn.addEventListener("click", handleProfileSave);
        if (dom.modalGuestBtn) dom.modalGuestBtn.addEventListener("click", handleProfileGuest);
        if (dom.modalProfile) {
            dom.modalProfile.addEventListener("click", e => {
                if (e.target === dom.modalProfile) closeProfileModal();
            });
        }

        // Home CTAs
        if (dom.btnHeroPlay) dom.btnHeroPlay.addEventListener("click", () => navigateTo("play"));
        if (dom.btnHeroMultiplayer) dom.btnHeroMultiplayer.addEventListener("click", () => navigateTo("multiplayer"));
        if (dom.btnHeroLeaderboard) dom.btnHeroLeaderboard.addEventListener("click", () => navigateTo("leaderboard"));
        if (dom.btnCardComputer) dom.btnCardComputer.addEventListener("click", () => navigateTo("play"));
        if (dom.btnCardMultiplayer) dom.btnCardMultiplayer.addEventListener("click", () => navigateTo("multiplayer"));

        // Play vs Computer Controls
        dom.diffButtons.forEach(btn => {
            if (btn) {
                btn.addEventListener("click", () => setComputerDifficulty(btn.dataset.difficulty));
            }
        });
        if (dom.btnRestartComputer) dom.btnRestartComputer.addEventListener("click", restartComputerRound);
        if (dom.btnResetScores) dom.btnResetScores.addEventListener("click", resetComputerScores);

        // Computer Board cell clicks
        dom.computerCells.forEach(cell => {
            cell.addEventListener("click", handleComputerCellClick);
        });

        // Multiplayer Controls
        if (dom.btnMpFind) dom.btnMpFind.addEventListener("click", handleMultiplayerFindMatch);
        if (dom.btnMpCancel) dom.btnMpCancel.addEventListener("click", handleMultiplayerCancelMatch);
        if (dom.btnMpPlayAgain) dom.btnMpPlayAgain.addEventListener("click", handleMultiplayerFindMatch);
        if (dom.btnMpLeave) dom.btnMpLeave.addEventListener("click", handleMultiplayerLeaveArena);

        // Multiplayer Board cell clicks
        dom.mpCells.forEach(cell => {
            cell.addEventListener("click", handleMultiplayerCellClick);
        });

        // Leaderboard & History buttons
        if (dom.btnRefreshLeaderboard) dom.btnRefreshLeaderboard.addEventListener("click", loadLeaderboardData);
        if (dom.btnHistoryPrev) dom.btnHistoryPrev.addEventListener("click", () => changeHistoryPage(-1));
        if (dom.btnHistoryNext) dom.btnHistoryNext.addEventListener("click", () => changeHistoryPage(1));
    }

    /**
     * Navigates to a specific view.
     */
    function navigateTo(viewId) {
        const targetView = document.getElementById(`view-${viewId}`);
        if (!targetView) {
            viewId = "home";
        }

        state.currentView = viewId;

        // Update active class on nav links
        dom.navLinks.forEach(link => {
            link.classList.toggle("active", link.dataset.view === viewId);
        });

        // Show matching view section
        dom.views.forEach(view => {
            view.classList.toggle("active-view", view.id === `view-${viewId}`);
        });

        // View-specific initialization triggers
        if (viewId === "play") {
            renderComputerBoard();
        } else if (viewId === "multiplayer") {
            renderMultiplayerBoard();
            if (window.TicTacToeSocket && !window.TicTacToeSocket.isConnected) {
                window.TicTacToeSocket.connect(window.TicTacToeApi.baseUrl);
            }
        } else if (viewId === "leaderboard") {
            loadLeaderboardData();
        } else if (viewId === "history") {
            loadUserHistoryData();
        }

        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    // ==========================================
    // USER PROFILE & MODAL
    // ==========================================
    function updateUserProfileDisplay() {
        if (state.user.username) {
            dom.navUsername.textContent = state.user.username;
            dom.userAvatar.textContent = state.user.username.charAt(0).toUpperCase();
            dom.userAvatar.style.background = "linear-gradient(135deg, var(--accent-primary), #818cf8)";
        } else {
            dom.navUsername.textContent = "Guest";
            dom.userAvatar.textContent = "?";
            dom.userAvatar.style.background = "linear-gradient(135deg, #64748b, #475569)";
        }
    }

    function openProfileModal() {
        dom.modalProfile.classList.add("open");
        dom.modalProfile.setAttribute("aria-hidden", "false");
        dom.modalUsernameInput.value = state.user.username || "";
        dom.modalStatusMsg.textContent = "";

        if (state.user.id) {
            dom.modalProfileStats.classList.remove("hidden");
            dom.modalProfileId.textContent = `#${state.user.id}`;
        } else {
            dom.modalProfileStats.classList.add("hidden");
        }
        dom.modalUsernameInput.focus();
    }

    function closeProfileModal() {
        dom.modalProfile.classList.remove("open");
        dom.modalProfile.setAttribute("aria-hidden", "true");
    }

    async function handleProfileSave() {
        const username = dom.modalUsernameInput.value.trim();
        if (username.length < 3 || username.length > 30) {
            dom.modalStatusMsg.textContent = "Username must be 3–30 characters.";
            dom.modalStatusMsg.style.color = "var(--accent-secondary)";
            return;
        }

        dom.modalStatusMsg.textContent = "Saving profile...";
        dom.modalStatusMsg.style.color = "var(--text-secondary)";

        try {
            const user = await window.TicTacToeApi.createUser(username);
            setUserProfile(user.id, user.username);
            dom.modalStatusMsg.textContent = `Welcome, ${user.username}!`;
            dom.modalStatusMsg.style.color = "var(--accent-green)";
            setTimeout(closeProfileModal, 900);
        } catch (err) {
            dom.modalStatusMsg.textContent = err.message || "Failed to save username.";
            dom.modalStatusMsg.style.color = "var(--accent-secondary)";
        }
    }

    function handleProfileGuest() {
        setUserProfile(null, null);
        dom.modalStatusMsg.textContent = "Switched to Guest mode.";
        dom.modalStatusMsg.style.color = "var(--text-secondary)";
        setTimeout(closeProfileModal, 700);
    }

    function setUserProfile(userId, username) {
        state.user.id = userId;
        state.user.username = username;

        if (userId && username) {
            localStorage.setItem("tictactoe_user_id", userId.toString());
            localStorage.setItem("tictactoe_username", username);
            if (window.TicTacToeSocket) {
                window.TicTacToeSocket.subscribeUserMatchFallback(userId);
            }
        } else {
            localStorage.removeItem("tictactoe_user_id");
            localStorage.removeItem("tictactoe_username");
        }

        updateUserProfileDisplay();
    }

    async function ensureUserForMultiplayer() {
        if (!state.user.id || !state.user.username) {
            const guestName = `Player_${Math.floor(1000 + Math.random() * 9000)}`;
            try {
                const user = await window.TicTacToeApi.createUser(guestName);
                setUserProfile(user.id, user.username);
            } catch (e) {
                console.error("Auto guest creation failed:", e);
            }
        }
    }

    // ==========================================
    // PLAYER VS COMPUTER (STATELESS REST)
    // ==========================================
    const DIFF_TOOLTIPS = {
        EASY: "Casual practice &bull; Random placement",
        MEDIUM: "Tactical &bull; Wins or blocks 1-step threats",
        HARD: "Unbeatable &bull; Optimal Minimax with Alpha-Beta Pruning"
    };

    function setComputerDifficulty(diff) {
        if (state.computerGame.difficulty === diff) return;
        state.computerGame.difficulty = diff;

        dom.diffButtons.forEach(btn => {
            if (!btn) return;
            const isActive = btn.dataset.difficulty === diff;
            btn.classList.toggle("active", isActive);
            btn.setAttribute("aria-checked", isActive ? "true" : "false");
        });

        if (dom.diffTooltip) {
            dom.diffTooltip.innerHTML = DIFF_TOOLTIPS[diff] || "";
        }

        const hasMoves = state.computerGame.board.some(c => c !== "");
        if (hasMoves || state.computerGame.status !== "IN_PROGRESS") {
            restartComputerRound();
        }
    }

    function restartComputerRound() {
        state.computerGame.board = [...INITIAL_BOARD];
        state.computerGame.status = "IN_PROGRESS";
        state.computerGame.isRequestPending = false;
        state.computerGame.errorMessage = null;
        state.computerGame.startedAt = null;
        state.computerGame.winningLine = null;
        renderComputerBoard();
    }

    function resetComputerScores() {
        state.computerGame.scores = { player: 0, computer: 0, draws: 0 };
        restartComputerRound();
    }

    function handleComputerCellClick(e) {
        const pos = parseInt(e.currentTarget.dataset.position, 10);
        const idx = parseInt(e.currentTarget.dataset.index, 10);

        if (state.computerGame.status !== "IN_PROGRESS" || state.computerGame.isRequestPending) {
            return;
        }
        if (state.computerGame.board[idx] !== "") {
            return;
        }

        state.computerGame.errorMessage = null;
        if (!state.computerGame.startedAt) {
            state.computerGame.startedAt = new Date().toISOString();
        }

        submitComputerMove(pos);
    }

    async function submitComputerMove(pos) {
        state.computerGame.isRequestPending = true;
        renderComputerBoard();

        const payload = {
            board: state.computerGame.board,
            move: pos,
            difficulty: state.computerGame.difficulty
        };

        if (state.user.id) {
            payload.userId = state.user.id;
            payload.startedAt = state.computerGame.startedAt;
        }

        try {
            const data = await window.TicTacToeApi.sendComputerMove(payload);
            const wasInProgress = state.computerGame.status === "IN_PROGRESS";

            state.computerGame.board = data.board;
            state.computerGame.status = data.status;
            state.computerGame.winningLine = data.winningLine || null;

            if (wasInProgress) {
                if (data.status === "PLAYER_WON") {
                    state.computerGame.scores.player++;
                } else if (data.status === "COMPUTER_WON") {
                    state.computerGame.scores.computer++;
                } else if (data.status === "DRAW") {
                    state.computerGame.scores.draws++;
                }
            }
        } catch (err) {
            state.computerGame.errorMessage = err.message || "Failed to process move.";
        } finally {
            state.computerGame.isRequestPending = false;
            renderComputerBoard();
        }
    }

    function renderComputerBoard() {
        // Render 9 cells
        dom.computerCells.forEach((cell, idx) => {
            const val = state.computerGame.board[idx];
            cell.textContent = val;

            cell.classList.remove("player-x", "computer-o", "winning-cell");
            if (val === "X") cell.classList.add("player-x");
            else if (val === "O") cell.classList.add("computer-o");

            const pos = idx + 1;
            if (state.computerGame.winningLine && state.computerGame.winningLine.includes(pos)) {
                cell.classList.add("winning-cell");
            }

            cell.setAttribute("aria-label", `Position ${pos}, ${val || "empty"}`);
            cell.disabled = val !== "" || state.computerGame.status !== "IN_PROGRESS" || state.computerGame.isRequestPending;
        });

        // Scores
        dom.scorePlayer.textContent = state.computerGame.scores.player;
        dom.scoreComputer.textContent = state.computerGame.scores.computer;
        dom.scoreDraws.textContent = state.computerGame.scores.draws;

        // Status Banner
        const statusEl = dom.computerStatus;
        statusEl.className = "status-banner";

        if (state.computerGame.errorMessage) {
            statusEl.textContent = state.computerGame.errorMessage;
            statusEl.classList.add("status-error");
            return;
        }

        if (state.computerGame.isRequestPending) {
            statusEl.textContent = "AI is evaluating optimal move...";
            statusEl.classList.add("status-thinking");
            return;
        }

        switch (state.computerGame.status) {
            case "PLAYER_WON":
                statusEl.textContent = "Victory! You defeated the AI! 🎉";
                statusEl.classList.add("status-player-won");
                break;
            case "COMPUTER_WON":
                statusEl.textContent = "Defeat! The AI outplayed you.";
                statusEl.classList.add("status-computer-won");
                break;
            case "DRAW":
                statusEl.textContent = "Game Draw! Perfectly matched.";
                statusEl.classList.add("status-draw");
                break;
            default:
                statusEl.textContent = "Your turn! Select an empty cell.";
                break;
        }
    }

    // ==========================================
    // ONLINE MULTIPLAYER (WEBSOCKET + STOMP)
    // ==========================================
    function initSocketListeners() {
        if (!window.TicTacToeSocket) return;

        window.TicTacToeSocket.onConnect(() => {
            updateMultiplayerConnectionBadge("CONNECTED");
            if (state.user.id) {
                window.TicTacToeSocket.subscribeUserMatchFallback(state.user.id);
            }
        });

        window.TicTacToeSocket.onDisconnect(() => {
            updateMultiplayerConnectionBadge("OFFLINE");
            state.multiplayer.isQueueing = false;
            dom.mpSpinner.classList.remove("searching");
            renderMultiplayerBoard();
        });

        window.TicTacToeSocket.onMatchFound(match => {
            handleMatchFound(match);
        });

        window.TicTacToeSocket.onGameState(gameState => {
            handleGameStateUpdate(gameState);
        });

        window.TicTacToeSocket.onError(err => {
            console.warn("WebSocket Error:", err);
            state.multiplayer.errorMessage = err.errorMessage || "Server error occurred.";
            renderMultiplayerBoard();
        });
    }

    function updateMultiplayerConnectionBadge(status) {
        if (!dom.mpConnectionBadge) return;
        dom.mpConnectionBadge.className = "connection-badge";

        if (status === "CONNECTED") {
            dom.mpConnectionBadge.textContent = "Online";
            dom.mpConnectionBadge.classList.add("badge-connected");
        } else if (status === "CONNECTING") {
            dom.mpConnectionBadge.textContent = "Connecting...";
            dom.mpConnectionBadge.classList.add("badge-connecting");
        } else {
            dom.mpConnectionBadge.textContent = "Offline";
            dom.mpConnectionBadge.classList.add("badge-disconnected");
        }
    }

    async function handleMultiplayerFindMatch() {
        await ensureUserForMultiplayer();

        if (!window.TicTacToeSocket.isConnected) {
            updateMultiplayerConnectionBadge("CONNECTING");
            window.TicTacToeSocket.connect(window.TicTacToeApi.baseUrl);
            setTimeout(handleMultiplayerFindMatch, 800);
            return;
        }

        state.multiplayer.isQueueing = true;
        state.multiplayer.inMatch = false;
        state.multiplayer.errorMessage = null;

        dom.btnMpFind.classList.add("hidden");
        dom.btnMpCancel.classList.remove("hidden");
        dom.btnMpPlayAgain.classList.add("hidden");
        dom.mpSpinner.classList.add("searching");
        dom.mpQueueMsg.textContent = "Searching for an available player in queue...";
        dom.mpStatus.textContent = "Matchmaking in progress...";
        dom.mpStatus.className = "status-banner status-thinking";

        try {
            window.TicTacToeSocket.joinMatchmaking(state.user.id, state.user.username);
        } catch (err) {
            state.multiplayer.errorMessage = err.message;
            state.multiplayer.isQueueing = false;
            renderMultiplayerBoard();
        }
    }

    function handleMultiplayerCancelMatch() {
        if (!state.multiplayer.isQueueing) return;

        state.multiplayer.isQueueing = false;
        dom.btnMpFind.classList.remove("hidden");
        dom.btnMpCancel.classList.add("hidden");
        dom.mpSpinner.classList.remove("searching");
        dom.mpQueueMsg.textContent = "Matchmaking cancelled.";
        dom.mpStatus.textContent = "Click 'Find Match' to queue for an opponent.";
        dom.mpStatus.className = "status-banner";

        if (state.user.id) {
            window.TicTacToeSocket.leaveMatchmaking(state.user.id);
        }
    }

    function handleMultiplayerLeaveArena() {
        if (state.multiplayer.isQueueing) {
            handleMultiplayerCancelMatch();
        }
        if (window.TicTacToeSocket) {
            window.TicTacToeSocket.unsubscribeGameSession();
        }
        state.multiplayer.inMatch = false;
        state.multiplayer.gameId = null;
        state.multiplayer.board = [...INITIAL_BOARD];
        dom.mpMatchStrip.classList.add("hidden");
        navigateTo("home");
    }

    function handleMatchFound(match) {
        state.multiplayer.isQueueing = false;
        state.multiplayer.inMatch = true;
        state.multiplayer.gameId = match.gameId;
        state.multiplayer.yourSymbol = match.yourSymbol;
        state.multiplayer.opponentUsername = match.opponentUsername;
        state.multiplayer.currentTurn = match.currentTurn;
        state.multiplayer.board = match.board || [...INITIAL_BOARD];
        state.multiplayer.status = match.status || "IN_PROGRESS";
        state.multiplayer.winningLine = null;
        state.multiplayer.errorMessage = null;

        dom.btnMpFind.classList.add("hidden");
        dom.btnMpCancel.classList.add("hidden");
        dom.btnMpPlayAgain.classList.add("hidden");
        dom.mpSpinner.classList.remove("searching");
        dom.mpQueueMsg.textContent = "Opponent matched! Game starting.";

        dom.mpMatchStrip.classList.remove("hidden");
        dom.mpYouSymbol.textContent = match.yourSymbol;
        dom.mpOppName.textContent = match.opponentUsername;

        window.TicTacToeSocket.subscribeGameSession(match.gameId);
        renderMultiplayerBoard();
    }

    function handleGameStateUpdate(msg) {
        if (msg.type === "PLAYER_DISCONNECTED") {
            state.multiplayer.status = "ABANDONED";
            state.multiplayer.errorMessage = msg.message || "Opponent disconnected. Match abandoned.";
            state.multiplayer.inMatch = false;
            dom.btnMpPlayAgain.classList.remove("hidden");
            renderMultiplayerBoard();
            return;
        }

        state.multiplayer.board = msg.board;
        state.multiplayer.status = msg.status;
        state.multiplayer.currentTurn = msg.currentTurn;
        state.multiplayer.winningLine = msg.winningLine || null;

        if (msg.type === "GAME_FINISHED") {
            state.multiplayer.inMatch = false;
            dom.btnMpPlayAgain.classList.remove("hidden");
            dom.mpQueueMsg.textContent = "Match completed.";
        }

        renderMultiplayerBoard();
    }

    function handleMultiplayerCellClick(e) {
        const pos = parseInt(e.currentTarget.dataset.position, 10);
        const idx = parseInt(e.currentTarget.dataset.index, 10);

        if (!state.multiplayer.inMatch || state.multiplayer.status !== "IN_PROGRESS") {
            return;
        }
        if (state.multiplayer.board[idx] !== "") {
            return;
        }
        if (state.multiplayer.currentTurn !== state.multiplayer.yourSymbol) {
            state.multiplayer.errorMessage = `Not your turn! Waiting for ${state.multiplayer.opponentUsername}...`;
            renderMultiplayerBoard();
            return;
        }

        state.multiplayer.errorMessage = null;

        try {
            window.TicTacToeSocket.sendMove(state.multiplayer.gameId, pos, state.user.id);
        } catch (err) {
            state.multiplayer.errorMessage = err.message;
            renderMultiplayerBoard();
        }
    }

    function renderMultiplayerBoard() {
        const isMyTurn = state.multiplayer.inMatch && 
                         state.multiplayer.status === "IN_PROGRESS" && 
                         state.multiplayer.currentTurn === state.multiplayer.yourSymbol;

        // Render 9 cells
        dom.mpCells.forEach((cell, idx) => {
            const val = state.multiplayer.board[idx];
            cell.textContent = val;

            cell.classList.remove("player-x", "computer-o", "winning-cell");
            if (val === "X") cell.classList.add("player-x");
            else if (val === "O") cell.classList.add("computer-o");

            const pos = idx + 1;
            if (state.multiplayer.winningLine && state.multiplayer.winningLine.includes(pos)) {
                cell.classList.add("winning-cell");
            }

            cell.setAttribute("aria-label", `Position ${pos}, ${val || "empty"}`);
            cell.disabled = val !== "" || !isMyTurn;
        });

        // Status Banner
        const statusEl = dom.mpStatus;
        statusEl.className = "status-banner";

        if (state.multiplayer.errorMessage) {
            statusEl.textContent = state.multiplayer.errorMessage;
            statusEl.classList.add("status-error");
            return;
        }

        if (!state.multiplayer.inMatch) {
            if (state.multiplayer.isQueueing) {
                statusEl.textContent = "Finding an opponent in queue...";
                statusEl.classList.add("status-thinking");
            } else {
                statusEl.textContent = "Click 'Find Match' to challenge another player.";
            }
            return;
        }

        switch (state.multiplayer.status) {
            case "PLAYER_ONE_WON":
            case "PLAYER_TWO_WON":
                const youWon = (state.multiplayer.status === "PLAYER_ONE_WON" && state.multiplayer.yourSymbol === "X") ||
                               (state.multiplayer.status === "PLAYER_TWO_WON" && state.multiplayer.yourSymbol === "O");
                if (youWon) {
                    statusEl.textContent = "Victory! You won the multiplayer match! 🎉";
                    statusEl.classList.add("status-player-won");
                } else {
                    statusEl.textContent = `Defeat! ${state.multiplayer.opponentUsername} won.`;
                    statusEl.classList.add("status-computer-won");
                }
                break;
            case "DRAW":
                statusEl.textContent = "Draw! A hard-fought battle.";
                statusEl.classList.add("status-draw");
                break;
            case "ABANDONED":
                statusEl.textContent = "Match abandoned.";
                statusEl.classList.add("status-error");
                break;
            default:
                if (isMyTurn) {
                    statusEl.textContent = `Your Turn (${state.multiplayer.yourSymbol})! Click any empty cell.`;
                } else {
                    statusEl.textContent = `Waiting for ${state.multiplayer.opponentUsername || "Opponent"} (${state.multiplayer.currentTurn})...`;
                    statusEl.classList.add("status-thinking");
                }
                break;
        }
    }

    // ==========================================
    // LEADERBOARD VIEW
    // ==========================================
    async function loadLeaderboardData() {
        const tbody = dom.leaderboardTableBody;
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading rankings...</td></tr>`;

        try {
            const data = await window.TicTacToeApi.fetchLeaderboard(15);
            if (!data || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 2rem;">No ranked games recorded yet. Be the first!</td></tr>`;
                dom.statTotalPlayers.textContent = "0";
                dom.statTotalMatches.textContent = "0";
                dom.statTotalWins.textContent = "0";
                return;
            }

            let totalMatches = 0;
            let totalWins = 0;

            tbody.innerHTML = data.map(entry => {
                totalMatches += entry.gamesPlayed;
                totalWins += entry.wins;

                let rankBadge = `<strong>${entry.rank}</strong>`;
                if (entry.rank === 1) rankBadge = `<span class="rank-badge rank-1">1</span>`;
                else if (entry.rank === 2) rankBadge = `<span class="rank-badge rank-2">2</span>`;
                else if (entry.rank === 3) rankBadge = `<span class="rank-badge rank-3">3</span>`;

                return `
                    <tr>
                        <td>${rankBadge}</td>
                        <td><strong style="color: var(--text-primary);">${escapeHtml(entry.username)}</strong></td>
                        <td style="text-align: right;">${entry.gamesPlayed}</td>
                        <td style="text-align: right; color: var(--accent-green); font-weight: 700;">${entry.wins}</td>
                        <td style="text-align: right; color: var(--accent-secondary);">${entry.losses}</td>
                        <td style="text-align: right; color: var(--accent-amber);">${entry.draws}</td>
                        <td style="text-align: right; font-weight: 700;">${entry.winRate.toFixed(1)}%</td>
                    </tr>
                `;
            }).join("");

            dom.statTotalPlayers.textContent = data.length.toString();
            dom.statTotalMatches.textContent = totalMatches.toString();
            dom.statTotalWins.textContent = totalWins.toString();

        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--accent-secondary); padding: 2rem;">Unable to load leaderboard. Please verify backend connection.</td></tr>`;
        }
    }

    // ==========================================
    // HISTORY VIEW
    // ==========================================
    async function loadUserHistoryData() {
        const feed = dom.historyFeed;

        if (!state.user.id) {
            feed.innerHTML = `
                <div style="text-align: center; padding: 3rem 1rem; color: var(--text-muted);">
                    <p style="font-size: 1.1rem; margin-bottom: 1rem;">No player profile identified.</p>
                    <button type="button" class="btn btn-primary" onclick="document.getElementById('btn-profile').click()">
                        Log In / Set Username
                    </button>
                </div>
            `;
            dom.historyPagination.classList.add("hidden");
            return;
        }

        feed.innerHTML = `<p style="text-align: center; color: var(--text-muted); padding: 2rem;">Loading match history...</p>`;

        try {
            const page = await window.TicTacToeApi.fetchUserHistory(state.user.id, state.history.currentPage, state.history.pageSize);
            if (!page.content || page.content.length === 0) {
                feed.innerHTML = `
                    <div style="text-align: center; padding: 3rem 1rem; color: var(--text-muted);">
                        <p style="font-size: 1.1rem; margin-bottom: 1rem;">No completed matches recorded yet.</p>
                        <button type="button" class="btn btn-primary" onclick="window.location.hash='#play'">
                            Play Your First Game
                        </button>
                    </div>
                `;
                dom.historyPagination.classList.add("hidden");
                return;
            }

            state.history.totalPages = page.totalPages || 1;

            feed.innerHTML = page.content.map(game => {
                const outcomeClass = game.outcome === "WIN" 
                    ? "outcome-win" 
                    : (game.outcome === "LOSS" ? "outcome-loss" : "outcome-draw");
                
                const modeLabel = game.gameMode === "MULTIPLAYER" 
                    ? `⚔️ Online vs ${escapeHtml(game.opponent)}`
                    : `🤖 vs AI (${game.difficulty || "Computer"})`;

                const dateStr = game.completedAt ? new Date(game.completedAt).toLocaleString() : "Recently";

                return `
                    <div class="history-card">
                        <div class="history-info">
                            <span class="history-mode">${modeLabel}</span>
                            <span class="history-meta">Match #${game.gameId} &bull; ${dateStr}</span>
                        </div>
                        <span class="outcome-badge ${outcomeClass}">${game.outcome}</span>
                    </div>
                `;
            }).join("");

            // Update Pagination
            dom.historyPagination.classList.remove("hidden");
            dom.historyPageInfo.textContent = `Page ${state.history.currentPage + 1} of ${state.history.totalPages}`;
            dom.btnHistoryPrev.disabled = state.history.currentPage === 0;
            dom.btnHistoryNext.disabled = state.history.currentPage >= state.history.totalPages - 1;

        } catch (err) {
            feed.innerHTML = `<p style="text-align: center; color: var(--accent-secondary); padding: 2rem;">Unable to load history. Please try again later.</p>`;
            dom.historyPagination.classList.add("hidden");
        }
    }

    function changeHistoryPage(delta) {
        state.history.currentPage += delta;
        if (state.history.currentPage < 0) state.history.currentPage = 0;
        loadUserHistoryData();
    }

    function escapeHtml(str) {
        if (!str) return "";
        return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }

    // Expose app controller and initialize on DOM ready
    window.TicTacToeApp = { state, navigateTo };
    document.addEventListener("DOMContentLoaded", init);

})(window, document);
