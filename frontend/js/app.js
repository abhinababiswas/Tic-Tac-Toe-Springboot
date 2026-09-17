/**
 * Tic-Tac-Toe Frontend Application (MVP 1)
 * 
 * Architectural Responsibilities:
 * - Render board state, session scores, and game status.
 * - Capture player interaction and difficulty selection.
 * - Maintain in-browser session score (Player, Computer, Draws).
 * - Submit state + move to the stateless Spring Boot REST API (POST /api/game/move).
 * - Render authoritative backend responses without duplicating game rules in JS.
 */

// Logical board representation: 9 elements (index 0..8 mapped to position 1..9)
const INITIAL_BOARD = ["", "", "", "", "", "", "", "", ""];

// Endpoint selection:
// 1. If hosted on Spring Boot (port 8080) or any cloud domain (e.g. *.onrender.com), use relative paths.
// 2. If opened from local filesystem (file://) or a separate local dev server (5500/3000), target localhost:8080.
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
    board: [...INITIAL_BOARD],
    difficulty: "EASY", // Default difficulty
    status: "IN_PROGRESS",
    isRequestPending: false,
    errorMessage: null,
    userId: localStorage.getItem("tictactoe_user_id") ? parseInt(localStorage.getItem("tictactoe_user_id"), 10) : null,
    username: localStorage.getItem("tictactoe_username") || null,
    gameStartedAt: null,
    scores: {
        player: 0,
        computer: 0,
        draws: 0
    }
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

/**
 * Initializes the application on page load.
 */
function init() {
    // Difficulty toggle listeners
    diffEasyBtn.addEventListener("click", () => setDifficulty("EASY"));
    diffMediumBtn.addEventListener("click", () => setDifficulty("MEDIUM"));
    if (diffHardBtn) {
        diffHardBtn.addEventListener("click", () => setDifficulty("HARD"));
    }

    // Reset game listener
    btnRestart.addEventListener("click", startNewGame);

    // Board cell click listeners
    cells.forEach(cell => {
        cell.addEventListener("click", handleCellClick);
    });

    // Profile & Stats Panel Listeners
    const btnToggleAuth = document.getElementById("btn-toggle-auth");
    const btnToggleStats = document.getElementById("btn-toggle-stats");
    const btnClosePanel = document.getElementById("btn-close-panel");
    const btnSubmitUser = document.getElementById("btn-submit-user");
    const btnGuestMode = document.getElementById("btn-guest-mode");

    if (btnToggleAuth) {
        btnToggleAuth.addEventListener("click", () => toggleStatsPanel(true));
    }
    if (btnToggleStats) {
        btnToggleStats.addEventListener("click", () => toggleStatsPanel(false));
    }
    if (btnClosePanel) {
        btnClosePanel.addEventListener("click", closeStatsPanel);
    }
    if (btnSubmitUser) {
        btnSubmitUser.addEventListener("click", handleUserSubmit);
    }
    if (btnGuestMode) {
        btnGuestMode.addEventListener("click", handleGuestMode);
    }

    updateProfileDisplay();
    render();
}

/**
 * Sets the active AI difficulty level and updates UI controls.
 * Per MVP UX specifications, changing difficulty resets the current board
 * for a new game while preserving session scores.
 * @param {"EASY" | "MEDIUM" | "HARD"} difficulty 
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

    // Changing difficulty resets the board state while preserving scores
    const hasMoves = gameState.board.some(cell => cell !== "");
    if (hasMoves || gameState.status !== "IN_PROGRESS") {
        startNewGame();
    }
}

/**
 * Handles cell click events initiated by the human player.
 * Validates cell emptiness and delegates to authoritative backend.
 * @param {MouseEvent} event 
 */
function handleCellClick(event) {
    const position = parseInt(event.currentTarget.dataset.position, 10);
    const index = parseInt(event.currentTarget.dataset.index, 10);

    // Prevent interaction if game is over, request is pending, or cell is occupied
    if (gameState.status !== "IN_PROGRESS" || gameState.isRequestPending) {
        return;
    }

    if (gameState.board[index] !== "") {
        return;
    }

    // Clear any previous transient error
    gameState.errorMessage = null;

    if (!gameState.gameStartedAt) {
        gameState.gameStartedAt = new Date().toISOString();
    }

    // Send move to authoritative backend
    sendMoveToBackend(position);
}

/**
 * Submits player move to the stateless Spring Boot API.
 * @param {number} position 1-based board position (1-9)
 */
async function sendMoveToBackend(position) {
    // Lock board interaction and show thinking status (prevents double-clicks)
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
                // Non-JSON response fallback
            }
            gameState.errorMessage = errorMsg;
            return;
        }

        const data = await response.json();

        // Capture prior status before updating to guarantee score integrity
        const wasInProgress = gameState.status === "IN_PROGRESS";

        // Authoritatively update state from backend response
        gameState.board = data.board;
        gameState.status = data.status;
        gameState.errorMessage = null;

        // Update session scores exactly once upon transitioning from IN_PROGRESS
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

        cell.classList.remove("player-x", "computer-o");
        if (value === "X") {
            cell.classList.add("player-x");
        } else if (value === "O") {
            cell.classList.add("computer-o");
        }

        // Accessibility label
        const position = index + 1;
        const stateLabel = value ? value : "empty";
        cell.setAttribute("aria-label", `Position ${position}, ${stateLabel}`);

        // Cell is disabled if occupied, if request is in flight, or if game is concluded
        cell.disabled = value !== "" || gameState.status !== "IN_PROGRESS" || gameState.isRequestPending;
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
    // Clear all status modifier classes
    statusElement.className = "status-banner";

    if (gameState.errorMessage) {
        statusElement.textContent = gameState.errorMessage;
        statusElement.classList.add("status-error");
        return;
    }

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
        // Try creating new user
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

        // If username already exists (409), fetch existing profile
        if (createRes.status === 409) {
            const getRes = await fetch(`${API_USERS_URL}/by-username/${encodeURIComponent(username)}`);
            if (getRes.ok) {
                const user = await getRes.json();
                setUserProfile(user.id, user.username);
                msg.textContent = `Logged in as ${user.username}!`;
                msg.style.color = "var(--success-color)";
                input.value = "";
                return;
            }
        }

        const errData = await createRes.json().catch(() => ({}));
        msg.textContent = errData.message || "Failed to set username.";
        msg.style.color = "var(--computer-color)";

    } catch (err) {
        msg.textContent = "Backend connection failed. Verify server is running.";
        msg.style.color = "var(--computer-color)";
    }
}

function setUserProfile(id, username) {
    gameState.userId = id;
    gameState.username = username;
    localStorage.setItem("tictactoe_user_id", id.toString());
    localStorage.setItem("tictactoe_username", username);
    updateProfileDisplay();
    fetchLeaderboard();
    fetchUserHistory();
}

function handleGuestMode() {
    gameState.userId = null;
    gameState.username = null;
    localStorage.removeItem("tictactoe_user_id");
    localStorage.removeItem("tictactoe_username");
    updateProfileDisplay();

    const msg = document.getElementById("user-status-msg");
    if (msg) {
        msg.textContent = "Playing in guest mode (scores are not saved to database).";
        msg.style.color = "var(--draw-color)";
    }
}

/**
 * Fetches and displays persistent leaderboard rankings.
 */
async function fetchLeaderboard() {
    const tbody = document.getElementById("leaderboard-body");
    if (!tbody) return;

    try {
        const res = await fetch(`${API_LEADERBOARD_URL}?limit=10`);
        if (!res.ok) throw new Error("Leaderboard unavailable");

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
            return `
                <div class="history-item">
                    <span>vs ${escapeHtml(game.opponent)} (${game.difficulty})</span>
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

