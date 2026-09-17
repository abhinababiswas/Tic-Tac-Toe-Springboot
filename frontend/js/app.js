/**
 * Tic-Tac-Toe Frontend Application (MVP 2 - Phase 4)
 * 
 * Architectural Responsibilities:
 * - Render board state, session scores, and game status.
 * - Single-Player (vs Computer): Stateless Spring Boot REST API (POST /api/game/move).
 * - Online Multiplayer: Real-time WebSocket + STOMP messaging (/ws, /app, /topic, /user/queue).
 * - User Profiles, Leaderboard, and Persistent Game History.
 * - Server remains authoritative in all modes; zero client-side game rule calculations.
 */

// Logical board representation: 9 elements (index 0..8 mapped to position 1..9)
const INITIAL_BOARD = ["", "", "", "", "", "", "", "", ""];

// Endpoint selection:
const isCloudOrSpringBoot = window.location.protocol.startsWith("http") && (
    window.location.port === "8080" || 
    (window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1")
);
const API_BASE = isCloudOrSpringBoot ? "" : "http://localhost:8080";
const API_MOVE_URL = `${API_BASE}/api/game/move`;
const API_USERS_URL = `${API_BASE}/api/users`;
const API_LEADERBOARD_URL = `${API_BASE}/api/leaderboard`;

// In-memory client state
const gameState = {
    gameMode: "COMPUTER", // "COMPUTER" | "MULTIPLAYER"
    board: [...INITIAL_BOARD],
    difficulty: "EASY",   // Default difficulty for Computer
    status: "IN_PROGRESS",
    isRequestPending: false,
    errorMessage: null,
    userId: localStorage.getItem("tictactoe_user_id") ? parseInt(localStorage.getItem("tictactoe_user_id"), 10) : null,
    username: localStorage.getItem("tictactoe_username") || null,
    gameStartedAt: null,
    winningLine: null,
    scores: {
        player: 0,
        computer: 0,
        draws: 0
    }
};

// Multiplayer Runtime State
const mpState = {
    stompClient: null,
    isConnected: false,
    isQueueing: false,
    inMatch: false,
    gameId: null,
    yourSymbol: null,        // "X" or "O"
    opponentUsername: null,
    currentTurn: null,       // "X" or "O"
    gameSubscription: null
};

// DOM References
const boardElement = document.getElementById("board");
const cells = document.querySelectorAll(".cell");
const statusElement = document.getElementById("game-status");
const btnRestart = document.getElementById("btn-restart");
const diffEasyBtn = document.getElementById("diff-easy");
const diffMediumBtn = document.getElementById("diff-medium");
const diffHardBtn = document.getElementById("diff-hard");
const scorePlayerElement = document.getElementById("score-player");
const scoreComputerElement = document.getElementById("score-computer");
const scoreDrawsElement = document.getElementById("score-draws");
const scoreLabelPlayer = document.getElementById("score-label-player");
const scoreLabelComputer = document.getElementById("score-label-computer");

// Mode & Multiplayer DOM References
const modeComputerBtn = document.getElementById("mode-computer");
const modeMultiplayerBtn = document.getElementById("mode-multiplayer");
const difficultyPanel = document.getElementById("difficulty-panel");
const multiplayerPanel = document.getElementById("multiplayer-panel");
const mpConnectionBadge = document.getElementById("mp-connection-badge");
const mpQueueStatus = document.getElementById("mp-queue-status");
const btnFindMatch = document.getElementById("btn-find-match");
const btnCancelMatch = document.getElementById("btn-cancel-match");
const mpMatchInfo = document.getElementById("mp-match-info");
const mpPlayerSymbol = document.getElementById("mp-player-symbol");
const mpOpponentName = document.getElementById("mp-opponent-name");
const gameSubtitle = document.getElementById("game-subtitle");

/**
 * Initializes the application on page load.
 */
function init() {
    // Mode toggle listeners
    if (modeComputerBtn) {
        modeComputerBtn.addEventListener("click", () => switchMode("COMPUTER"));
    }
    if (modeMultiplayerBtn) {
        modeMultiplayerBtn.addEventListener("click", () => switchMode("MULTIPLAYER"));
    }

    // Difficulty toggle listeners (vs Computer)
    if (diffEasyBtn) diffEasyBtn.addEventListener("click", () => setDifficulty("EASY"));
    if (diffMediumBtn) diffMediumBtn.addEventListener("click", () => setDifficulty("MEDIUM"));
    if (diffHardBtn) diffHardBtn.addEventListener("click", () => setDifficulty("HARD"));

    // Reset game / Action button
    btnRestart.addEventListener("click", handleActionClick);

    // Board cell click listeners
    cells.forEach(cell => {
        cell.addEventListener("click", handleCellClick);
    });

    // Multiplayer actions
    if (btnFindMatch) btnFindMatch.addEventListener("click", handleFindMatch);
    if (btnCancelMatch) btnCancelMatch.addEventListener("click", handleCancelMatch);

    // Profile & Stats Panel Listeners
    const btnToggleAuth = document.getElementById("btn-toggle-auth");
    const btnToggleStats = document.getElementById("btn-toggle-stats");
    const btnClosePanel = document.getElementById("btn-close-panel");
    const btnSubmitUser = document.getElementById("btn-submit-user");
    const btnGuestMode = document.getElementById("btn-guest-mode");

    if (btnToggleAuth) btnToggleAuth.addEventListener("click", () => toggleStatsPanel(true));
    if (btnToggleStats) btnToggleStats.addEventListener("click", () => toggleStatsPanel(false));
    if (btnClosePanel) btnClosePanel.addEventListener("click", closeStatsPanel);
    if (btnSubmitUser) btnSubmitUser.addEventListener("click", handleUserSubmit);
    if (btnGuestMode) btnGuestMode.addEventListener("click", handleGuestMode);

    updateProfileDisplay();
    render();
}

/**
 * Switches between Player vs Computer (REST) and Online Multiplayer (STOMP).
 */
function switchMode(mode) {
    if (gameState.gameMode === mode) return;

    gameState.gameMode = mode;
    startNewGame();

    if (mode === "COMPUTER") {
        modeComputerBtn.classList.add("active");
        modeMultiplayerBtn.classList.remove("active");
        difficultyPanel.classList.remove("hidden");
        multiplayerPanel.classList.add("hidden");
        if (gameSubtitle) gameSubtitle.textContent = "Player vs Computer";
        if (scoreLabelPlayer) scoreLabelPlayer.textContent = "Player (X)";
        if (scoreLabelComputer) scoreLabelComputer.textContent = "Computer (O)";
        btnRestart.textContent = "New Game";
    } else {
        modeMultiplayerBtn.classList.add("active");
        modeComputerBtn.classList.remove("active");
        difficultyPanel.classList.add("hidden");
        multiplayerPanel.classList.remove("hidden");
        if (gameSubtitle) gameSubtitle.textContent = "Online Multiplayer (WebSocket + STOMP)";
        if (scoreLabelPlayer) scoreLabelPlayer.textContent = "You";
        if (scoreLabelComputer) scoreLabelComputer.textContent = "Opponent";
        btnRestart.textContent = "New Match";

        // Ensure WebSocket is connected
        connectWebSocket();
    }
    render();
}

/**
 * Connects to the Spring Boot WebSocket & STOMP endpoint (/ws).
 */
function connectWebSocket() {
    if (mpState.isConnected || mpState.stompClient) {
        return;
    }

    updateConnectionBadge("CONNECTING");

    try {
        let socket;
        if (typeof SockJS !== "undefined") {
            const socketUrl = `${API_BASE}/ws`;
            socket = new SockJS(socketUrl);
        } else {
            const wsProto = window.location.protocol === "https:" ? "wss:" : "ws:";
            const wsHost = isCloudOrSpringBoot ? window.location.host : "localhost:8080";
            socket = new WebSocket(`${wsProto}//${wsHost}/ws`);
        }

        if (typeof Stomp !== "undefined") {
            mpState.stompClient = Stomp.over(socket);
            mpState.stompClient.debug = null; // Suppress noisy logs in production

            mpState.stompClient.connect({}, onStompConnected, onStompError);
        } else {
            console.warn("STOMP client library not loaded.");
            updateConnectionBadge("OFFLINE");
        }
    } catch (e) {
        console.error("Failed to initialize WebSocket:", e);
        updateConnectionBadge("OFFLINE");
    }
}

function onStompConnected(frame) {
    mpState.isConnected = true;
    updateConnectionBadge("CONNECTED");
    if (mpQueueStatus) mpQueueStatus.textContent = "Ready to match";

    // Subscribe to private match notifications
    mpState.stompClient.subscribe("/user/queue/match", message => {
        const payload = JSON.parse(message.body);
        onMatchFound(payload);
    });

    // Subscribe to private error messages
    mpState.stompClient.subscribe("/user/queue/errors", message => {
        const payload = JSON.parse(message.body);
        onWebSocketError(payload);
    });

    // Also subscribe to topic fallback if userId is known
    if (gameState.userId) {
        mpState.stompClient.subscribe(`/topic/match/${gameState.userId}`, message => {
            const payload = JSON.parse(message.body);
            onMatchFound(payload);
        });
    }
}

function onStompError(error) {
    mpState.isConnected = false;
    mpState.stompClient = null;
    updateConnectionBadge("OFFLINE");
    if (mpQueueStatus) mpQueueStatus.textContent = "Disconnected";
}

function updateConnectionBadge(status) {
    if (!mpConnectionBadge) return;

    mpConnectionBadge.className = "badge";
    if (status === "CONNECTED") {
        mpConnectionBadge.textContent = "Online";
        mpConnectionBadge.classList.add("badge-connected");
    } else if (status === "CONNECTING") {
        mpConnectionBadge.textContent = "Connecting...";
        mpConnectionBadge.classList.add("badge-connecting");
    } else {
        mpConnectionBadge.textContent = "Offline";
        mpConnectionBadge.classList.add("badge-disconnected");
    }
}

/**
 * Initiates matchmaking queue join.
 */
async function handleFindMatch() {
    if (!mpState.isConnected) {
        connectWebSocket();
        setTimeout(handleFindMatch, 800);
        return;
    }

    // Ensure we have a user profile; auto-register guest if needed
    if (!gameState.userId || !gameState.username) {
        await autoRegisterGuest();
    }

    mpState.isQueueing = true;
    if (btnFindMatch) btnFindMatch.classList.add("hidden");
    if (btnCancelMatch) btnCancelMatch.classList.remove("hidden");
    if (mpQueueStatus) mpQueueStatus.textContent = "Searching for opponent...";
    statusElement.textContent = "Searching for an opponent in queue...";
    statusElement.className = "status-banner status-thinking";

    const payload = {
        userId: gameState.userId,
        username: gameState.username
    };

    mpState.stompClient.send("/app/matchmaking/join", {}, JSON.stringify(payload));
}

/**
 * Cancels active matchmaking queue.
 */
function handleCancelMatch() {
    if (!mpState.isQueueing) return;

    mpState.isQueueing = false;
    if (btnFindMatch) btnFindMatch.classList.remove("hidden");
    if (btnCancelMatch) btnCancelMatch.classList.add("hidden");
    if (mpQueueStatus) mpQueueStatus.textContent = "Queue cancelled";
    statusElement.textContent = "Matchmaking cancelled.";
    statusElement.className = "status-banner";

    if (mpState.stompClient && gameState.userId) {
        mpState.stompClient.send("/app/matchmaking/leave", {}, JSON.stringify({ userId: gameState.userId }));
    }
}

/**
 * Triggered when paired with an opponent.
 */
function onMatchFound(match) {
    mpState.isQueueing = false;
    mpState.inMatch = true;
    mpState.gameId = match.gameId;
    mpState.yourSymbol = match.yourSymbol;
    mpState.opponentUsername = match.opponentUsername;
    mpState.currentTurn = match.currentTurn;
    gameState.status = match.status || "IN_PROGRESS";
    gameState.board = match.board || [...INITIAL_BOARD];
    gameState.winningLine = null;

    if (btnFindMatch) btnFindMatch.classList.add("hidden");
    if (btnCancelMatch) btnCancelMatch.classList.add("hidden");
    if (mpQueueStatus) mpQueueStatus.textContent = "In Match";

    if (mpMatchInfo) {
        mpMatchInfo.classList.remove("hidden");
        if (mpPlayerSymbol) mpPlayerSymbol.innerHTML = `You: <strong style="color: var(--accent-color);">${match.yourSymbol}</strong>`;
        if (mpOpponentName) mpOpponentName.innerHTML = `Opponent: <strong>${escapeHtml(match.opponentUsername)}</strong>`;
    }

    // Subscribe to public match topic
    if (mpState.gameSubscription) {
        mpState.gameSubscription.unsubscribe();
    }
    mpState.gameSubscription = mpState.stompClient.subscribe(`/topic/game/${match.gameId}`, message => {
        const payload = JSON.parse(message.body);
        onGameStateUpdate(payload);
    });

    render();
}

/**
 * Handles real-time game state updates from server broadcast (/topic/game/{gameId}).
 */
function onGameStateUpdate(msg) {
    if (msg.type === "PLAYER_DISCONNECTED") {
        statusElement.textContent = msg.message || "Opponent disconnected. Match abandoned.";
        statusElement.className = "status-banner status-error";
        gameState.status = "ABANDONED";
        mpState.inMatch = false;
        render();
        return;
    }

    gameState.board = msg.board;
    gameState.status = msg.status;
    mpState.currentTurn = msg.currentTurn;
    gameState.winningLine = msg.winningLine || null;

    if (msg.type === "GAME_FINISHED") {
        mpState.inMatch = false;
        if (btnFindMatch) {
            btnFindMatch.classList.remove("hidden");
            btnFindMatch.textContent = "Play Again";
        }
        if (btnCancelMatch) btnCancelMatch.classList.add("hidden");
        if (mpQueueStatus) mpQueueStatus.textContent = "Match Finished";

        // Refresh stats
        fetchLeaderboard();
        if (gameState.userId) fetchUserHistory();
    }

    render();
}

function onWebSocketError(err) {
    console.warn("WebSocket Error:", err);
    statusElement.textContent = err.errorMessage || "An error occurred.";
    statusElement.className = "status-banner status-error";
}

/**
 * Auto-registers an anonymous guest with a unique username for multiplayer persistence.
 */
async function autoRegisterGuest() {
    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const guestUsername = `Player_${randomSuffix}`;

    try {
        const res = await fetch(API_USERS_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: guestUsername })
        });
        if (res.ok) {
            const user = await res.json();
            setUserProfile(user.id, user.username);
        }
    } catch (e) {
        console.error("Failed to auto-register guest:", e);
    }
}

/**
 * Sets the active AI difficulty level for Computer mode.
 */
function setDifficulty(difficulty) {
    if (gameState.difficulty === difficulty) return;

    gameState.difficulty = difficulty;

    [diffEasyBtn, diffMediumBtn, diffHardBtn].forEach(btn => {
        if (!btn) return;
        const isActive = btn.dataset.difficulty === difficulty;
        btn.classList.toggle("active", isActive);
        btn.setAttribute("aria-checked", isActive ? "true" : "false");
    });

    const hasMoves = gameState.board.some(cell => cell !== "");
    if (hasMoves || gameState.status !== "IN_PROGRESS") {
        startNewGame();
    }
}

/**
 * Unified action click handler (New Game / Find Match).
 */
function handleActionClick() {
    if (gameState.gameMode === "MULTIPLAYER") {
        if (mpState.inMatch) {
            // Leave current finished match
            mpState.inMatch = false;
            if (mpMatchInfo) mpMatchInfo.classList.add("hidden");
        }
        handleFindMatch();
    } else {
        startNewGame();
    }
}

/**
 * Handles cell click events initiated by the player.
 */
function handleCellClick(event) {
    const position = parseInt(event.currentTarget.dataset.position, 10);
    const index = parseInt(event.currentTarget.dataset.index, 10);

    if (gameState.gameMode === "MULTIPLAYER") {
        handleMultiplayerCellClick(position, index);
        return;
    }

    handleComputerCellClick(position, index);
}

/**
 * Cell click handler for Online Multiplayer (WebSocket STOMP).
 */
function handleMultiplayerCellClick(position, index) {
    if (!mpState.inMatch || gameState.status !== "IN_PROGRESS") {
        if (!mpState.inMatch) {
            statusElement.textContent = "Click 'Find Match' to queue for an opponent.";
            statusElement.className = "status-banner status-error";
        }
        return;
    }

    if (gameState.board[index] !== "") {
        return;
    }

    if (mpState.currentTurn !== mpState.yourSymbol) {
        statusElement.textContent = `Not your turn! Waiting for ${escapeHtml(mpState.opponentUsername || "opponent")}...`;
        statusElement.className = "status-banner status-error";
        return;
    }

    // Submit authoritative move via STOMP
    const payload = {
        position: position,
        userId: gameState.userId
    };

    mpState.stompClient.send(`/app/game/${mpState.gameId}/move`, {}, JSON.stringify(payload));
}

/**
 * Cell click handler for Player vs Computer (Stateless REST).
 */
function handleComputerCellClick(position, index) {
    if (gameState.status !== "IN_PROGRESS" || gameState.isRequestPending) {
        return;
    }

    if (gameState.board[index] !== "") {
        return;
    }

    gameState.errorMessage = null;

    if (!gameState.gameStartedAt) {
        gameState.gameStartedAt = new Date().toISOString();
    }

    sendMoveToBackend(position);
}

/**
 * Submits player move to the stateless Spring Boot API.
 */
async function sendMoveToBackend(position) {
    gameState.isRequestPending = true;
    render();

    const payload = {
        board: gameState.board,
        move: position,
        difficulty: gameState.difficulty
    };

    if (gameState.userId) {
        payload.userId = gameState.userId;
        payload.startedAt = gameState.gameStartedAt || new Date().toISOString();
    }

    try {
        const response = await fetch(API_MOVE_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            let errorMsg = "Unable to process your move. Please try again.";
            try {
                const errData = await response.json();
                if (errData && errData.message) {
                    errorMsg = errData.message;
                }
            } catch {
            }
            gameState.errorMessage = errorMsg;
            return;
        }

        const data = await response.json();
        const wasInProgress = gameState.status === "IN_PROGRESS";

        gameState.board = data.board;
        gameState.status = data.status;
        gameState.errorMessage = null;

        if (wasInProgress) {
            if (data.status === "PLAYER_WON") {
                gameState.scores.player++;
            } else if (data.status === "COMPUTER_WON") {
                gameState.scores.computer++;
            } else if (data.status === "DRAW") {
                gameState.scores.draws++;
            }
        }

    } catch (err) {
        gameState.errorMessage = "Unable to connect to the backend server. Please verify it is running on port 8080.";
    } finally {
        gameState.isRequestPending = false;
        render();
    }
}

/**
 * Renders the board, scores, accessible labels, and status banner.
 */
function render() {
    // Render 9 board cells
    cells.forEach((cell, index) => {
        const value = gameState.board[index];
        cell.textContent = value;

        cell.classList.remove("player-x", "computer-o", "winning-cell");
        if (value === "X") {
            cell.classList.add("player-x");
        } else if (value === "O") {
            cell.classList.add("computer-o");
        }

        const position = index + 1;
        if (gameState.winningLine && gameState.winningLine.includes(position)) {
            cell.classList.add("winning-cell");
        }

        const stateLabel = value ? value : "empty";
        cell.setAttribute("aria-label", `Position ${position}, ${stateLabel}`);

        // Cell is disabled if occupied, if request is in flight, or if game is concluded
        if (gameState.gameMode === "MULTIPLAYER") {
            const isMyTurn = mpState.inMatch && mpState.currentTurn === mpState.yourSymbol;
            cell.disabled = value !== "" || gameState.status !== "IN_PROGRESS" || !isMyTurn;
        } else {
            cell.disabled = value !== "" || gameState.status !== "IN_PROGRESS" || gameState.isRequestPending;
        }
    });

    // Render session scores
    scorePlayerElement.textContent = gameState.scores.player;
    scoreComputerElement.textContent = gameState.scores.computer;
    scoreDrawsElement.textContent = gameState.scores.draws;

    // Render game status banner
    renderStatus();
}

/**
 * Renders the status banner based on current application state.
 */
function renderStatus() {
    statusElement.className = "status-banner";

    if (gameState.errorMessage) {
        statusElement.textContent = gameState.errorMessage;
        statusElement.classList.add("status-error");
        return;
    }

    if (gameState.gameMode === "MULTIPLAYER") {
        if (!mpState.inMatch) {
            if (mpState.isQueueing) {
                statusElement.textContent = "Searching for an opponent in queue...";
                statusElement.classList.add("status-thinking");
            } else {
                statusElement.textContent = "Online Multiplayer: Click 'Find Match' to play.";
            }
            return;
        }

        switch (gameState.status) {
            case "PLAYER_ONE_WON":
            case "PLAYER_TWO_WON":
                const youWon = (gameState.status === "PLAYER_ONE_WON" && mpState.yourSymbol === "X") ||
                               (gameState.status === "PLAYER_TWO_WON" && mpState.yourSymbol === "O");
                if (youWon) {
                    statusElement.textContent = "Victory! You won the game! 🎉";
                    statusElement.classList.add("status-player-won");
                } else {
                    statusElement.textContent = `Defeat! ${escapeHtml(mpState.opponentUsername)} won.`;
                    statusElement.classList.add("status-computer-won");
                }
                break;
            case "DRAW":
                statusElement.textContent = "It's a Draw! Well played.";
                statusElement.classList.add("status-draw");
                break;
            case "ABANDONED":
                statusElement.textContent = "Opponent disconnected. Match abandoned.";
                statusElement.classList.add("status-error");
                break;
            case "IN_PROGRESS":
            default:
                if (mpState.currentTurn === mpState.yourSymbol) {
                    statusElement.textContent = `Your turn (${mpState.yourSymbol})! Make your move.`;
                } else {
                    statusElement.textContent = `Waiting for ${escapeHtml(mpState.opponentUsername)} (${mpState.currentTurn})...`;
                    statusElement.classList.add("status-thinking");
                }
                break;
        }
        return;
    }

    // Single-Player (vs Computer) Status
    if (gameState.isRequestPending) {
        statusElement.textContent = "Computer is thinking...";
        statusElement.classList.add("status-thinking");
        return;
    }

    switch (gameState.status) {
        case "PLAYER_WON":
            statusElement.textContent = "You Won! 🎉";
            statusElement.classList.add("status-player-won");
            break;
        case "COMPUTER_WON":
            statusElement.textContent = "Computer Won!";
            statusElement.classList.add("status-computer-won");
            break;
        case "DRAW":
            statusElement.textContent = "It's a Draw!";
            statusElement.classList.add("status-draw");
            break;
        case "IN_PROGRESS":
        default:
            statusElement.textContent = "Your turn! Select an empty cell.";
            break;
    }
}

/**
 * Resets the board for a new round while preserving session scores.
 */
function startNewGame() {
    gameState.board = [...INITIAL_BOARD];
    gameState.status = "IN_PROGRESS";
    gameState.isRequestPending = false;
    gameState.errorMessage = null;
    gameState.gameStartedAt = null;
    gameState.winningLine = null;
    render();
}

/**
 * Updates the header profile indicator.
 */
function updateProfileDisplay() {
    const playerDisplay = document.getElementById("player-display");
    const btnToggleAuth = document.getElementById("btn-toggle-auth");
    const historySection = document.getElementById("history-section");

    if (!playerDisplay) return;

    if (gameState.username) {
        playerDisplay.innerHTML = `Playing as: <strong style="color: var(--accent-color);">${escapeHtml(gameState.username)}</strong>`;
        if (btnToggleAuth) btnToggleAuth.textContent = "Switch";
        if (historySection) historySection.classList.remove("hidden");
    } else {
        playerDisplay.innerHTML = `Playing as: <strong>Guest</strong>`;
        if (btnToggleAuth) btnToggleAuth.textContent = "Log In";
        if (historySection) historySection.classList.add("hidden");
    }
}

/**
 * Toggles the visibility of the stats and player settings panel.
 */
function toggleStatsPanel(focusAuth = false) {
    const panel = document.getElementById("stats-panel");
    if (!panel) return;

    const isHidden = panel.classList.contains("hidden");
    if (isHidden) {
        panel.classList.remove("hidden");
        panel.setAttribute("aria-hidden", "false");
        fetchLeaderboard();
        if (gameState.userId) {
            fetchUserHistory();
        }
        if (focusAuth) {
            const input = document.getElementById("username-input");
            if (input) input.focus();
        }
    } else {
        closeStatsPanel();
    }
}

function closeStatsPanel() {
    const panel = document.getElementById("stats-panel");
    if (panel) {
        panel.classList.add("hidden");
        panel.setAttribute("aria-hidden", "true");
    }
}

/**
 * Handles user profile creation or sign-in.
 */
async function handleUserSubmit() {
    const input = document.getElementById("username-input");
    const msg = document.getElementById("user-status-msg");
    if (!input || !msg) return;

    const username = input.value.trim();
    if (username.length < 3 || username.length > 30) {
        msg.textContent = "Username must be between 3 and 30 characters.";
        msg.style.color = "var(--computer-color)";
        return;
    }

    msg.textContent = "Connecting...";
    msg.style.color = "var(--text-secondary)";

    try {
        const createRes = await fetch(API_USERS_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username })
        });

        if (createRes.status === 201) {
            const user = await createRes.json();
            setUserProfile(user.id, user.username);
            msg.textContent = `Welcome, ${user.username}!`;
            msg.style.color = "var(--success-color)";
            input.value = "";
            return;
        }

        if (createRes.status === 409) {
            const lookupRes = await fetch(`${API_USERS_URL}/by-username/${encodeURIComponent(username)}`);
            if (lookupRes.ok) {
                const user = await lookupRes.json();
                setUserProfile(user.id, user.username);
                msg.textContent = `Welcome back, ${user.username}!`;
                msg.style.color = "var(--success-color)";
                input.value = "";
                return;
            }
        }

        msg.textContent = "Unable to process username. Please try another.";
        msg.style.color = "var(--computer-color)";
    } catch {
        msg.textContent = "Server connection error.";
        msg.style.color = "var(--computer-color)";
    }
}

function handleGuestMode() {
    setUserProfile(null, null);
    const msg = document.getElementById("user-status-msg");
    if (msg) {
        msg.textContent = "Switched to Guest mode (anonymous).";
        msg.style.color = "var(--text-secondary)";
    }
}

function setUserProfile(userId, username) {
    gameState.userId = userId;
    gameState.username = username;

    if (userId && username) {
        localStorage.setItem("tictactoe_user_id", userId.toString());
        localStorage.setItem("tictactoe_username", username);
    } else {
        localStorage.removeItem("tictactoe_user_id");
        localStorage.removeItem("tictactoe_username");
    }

    updateProfileDisplay();
    fetchLeaderboard();
    if (userId) {
        fetchUserHistory();
    }
}

/**
 * Fetches and displays global leaderboard rankings.
 */
async function fetchLeaderboard() {
    const tbody = document.getElementById("leaderboard-body");
    if (!tbody) return;

    try {
        const res = await fetch(`${API_LEADERBOARD_URL}?limit=10`);
        if (!res.ok) throw new Error();

        const data = await res.json();
        if (!data || data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-muted">No completed games recorded yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = data.map(entry => `
            <tr>
                <td><strong>${entry.rank}</strong></td>
                <td>${escapeHtml(entry.username)}</td>
                <td style="color: var(--success-color);">${entry.wins}</td>
                <td style="color: var(--computer-color);">${entry.losses}</td>
                <td style="color: var(--draw-color);">${entry.draws}</td>
                <td>${entry.winRate.toFixed(1)}%</td>
            </tr>
        `).join("");

    } catch {
        tbody.innerHTML = `<tr><td colspan="6" class="text-muted">Unable to load leaderboard.</td></tr>`;
    }
}

/**
 * Fetches and displays personal persistent history.
 */
async function fetchUserHistory() {
    const historyList = document.getElementById("history-list");
    if (!historyList || !gameState.userId) return;

    try {
        const res = await fetch(`${API_USERS_URL}/${gameState.userId}/history?page=0&size=5`);
        if (!res.ok) return;

        const page = await res.json();
        if (!page.content || page.content.length === 0) {
            historyList.innerHTML = `<p class="text-muted">No completed games yet.</p>`;
            return;
        }

        historyList.innerHTML = page.content.map(game => {
            const outcomeClass = game.outcome === "WIN" 
                ? "outcome-win" 
                : (game.outcome === "LOSS" ? "outcome-loss" : "outcome-draw");
            const modeDesc = game.gameMode === "MULTIPLAYER" ? "Multiplayer" : (game.difficulty || "Computer");
            return `
                <div class="history-item">
                    <span>vs ${escapeHtml(game.opponent)} (${modeDesc})</span>
                    <span class="${outcomeClass}">${game.outcome}</span>
                </div>
            `;
        }).join("");

    } catch {
        historyList.innerHTML = `<p class="text-muted">Unable to load history.</p>`;
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", init);
