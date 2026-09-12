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
// 1. If hosted on Spring Boot (port 8080) or any cloud domain (e.g. *.onrender.com), use relative path "/api/game/move".
// 2. If opened from local filesystem (file://) or a separate local dev server (5500/3000), target localhost:8080.
const isCloudOrSpringBoot = window.location.protocol.startsWith("http") && (
    window.location.port === "8080" || 
    (window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1")
);
const API_URL = isCloudOrSpringBoot
    ? "/api/game/move"
    : "http://localhost:8080/api/game/move";

// In-memory client state
const gameState = {
    board: [...INITIAL_BOARD],
    difficulty: "EASY", // Default difficulty
    status: "IN_PROGRESS",
    isRequestPending: false,
    errorMessage: null,
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

    // Reset game listener
    btnRestart.addEventListener("click", startNewGame);

    // Board cell click listeners
    cells.forEach(cell => {
        cell.addEventListener("click", handleCellClick);
    });

    render();
}

/**
 * Sets the active AI difficulty level and updates UI controls.
 * Per MVP UX specifications, changing difficulty resets the current board
 * for a new game while preserving session scores.
 * @param {"EASY" | "MEDIUM"} difficulty 
 */
function setDifficulty(difficulty) {
    if (gameState.difficulty === difficulty) return;

    gameState.difficulty = difficulty;

    if (difficulty === "EASY") {
        diffEasyBtn.classList.add("active");
        diffEasyBtn.setAttribute("aria-checked", "true");
        diffMediumBtn.classList.remove("active");
        diffMediumBtn.setAttribute("aria-checked", "false");
    } else {
        diffMediumBtn.classList.add("active");
        diffMediumBtn.setAttribute("aria-checked", "true");
        diffEasyBtn.classList.remove("active");
        diffEasyBtn.setAttribute("aria-checked", "false");
    }

    // Changing difficulty resets the board state while preserving scores
    const hasMoves = gameState.board.some(cell => cell !== "");
    if (hasMoves || gameState.status !== "IN_PROGRESS") {
        startNewGame();
    }
}

/**
 * Handles cell click events initiated by the human player.
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

    try {
        const response = await fetch(API_URL, {
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
    render();
}

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", init);
