# Tic-Tac-Toe — Player vs Computer

A modern, responsive, web-based Tic-Tac-Toe application where a human player plays against an intelligent or randomized computer opponent. Built with a clean **Vanilla JavaScript** frontend and a robust, **stateless Java / Spring Boot** REST backend.

---

## 1. Overview & Key Features

* **Player vs Computer Gameplay**: Human plays as **`X`** (always moves first), and the Computer responds authoritatively as **`O`**.
* **Dual AI Difficulty Modes**:
  * **Easy**: Selects uniformly at random among available empty cells without strategic intent.
  * **Medium**: Rule-based tactical decision tree prioritizing **Immediate Win > Immediate Block > Random Selection**.
* **Stateless REST Architecture**: The server maintains zero persistent game sessions or in-memory game state. Each move payload contains the full board, enabling horizontal scalability and clean testability.
* **In-Memory Session Scoreboard**: Keeps running tally of Player Wins, Computer Wins, and Draws during the browser session without server-side storage.
* **New Game Functionality**: Reset round at any time while preserving current session scores.
* **Difficulty Switching Rule**: Switching difficulty cleanly resets the active board for a fresh match while preserving the session scoreboard.
* **Responsive & Accessible UI**: Modern dark theme built with CSS custom properties and CSS Grid. Fully keyboard navigable with clear ARIA labels and focus indicators.
* **Server-Authoritative Validation**: Comprehensive protection against illegal moves, occupied cell tampering, mismatched turn counts, or moves on concluded games.

---

## 2. Technology Stack

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Frontend** | HTML5 | Semantic markup, ARIA accessibility attributes, mobile viewport meta |
| | Vanilla CSS3 | Custom properties, CSS Grid 3x3 layout, responsive design |
| | Vanilla JavaScript (ES6+) | Native `fetch` API, state-driven rendering, zero external frameworks |
| **Backend** | Java 21 | Modern Java records/classes, immutable domain patterns (`Board`) |
| | Spring Boot 3.4.3 | `spring-boot-starter-web` (REST controllers, global advice) |
| | Maven | Build automation, dependency management, Surefire test runner |
| **Testing** | JUnit 5 & MockMvc | 64 automated unit, domain rule, AI scenario, and API integration tests |

---

## 3. System Architecture

The application follows a strictly layered, unidirectional flow from user interaction to domain decision-making:

```text
               Browser (Client)
                      |
                      | HTTP POST /api/game/move (JSON payload)
                      v
             Spring Boot REST API
                      |
                      v
               Game Controller        [controller/GameController.java]
                      |
                      v
                Game Service          [service/GameService.java]
                      |
                      v
                Game Engine           [engine/GameEngine.java]
                      |
                      v
              Computer Player         [engine/ComputerPlayer.java]
```

### Separation of Responsibilities

* **Frontend (Client)**:
  * Manages UI state, captures user clicks, displays thinking/error banners.
  * Maintains in-browser session score (Player, Computer, Draws).
  * Sends current 9-cell board state + player move to the backend.
  * Renders authoritative board and status returned by the server.
* **Backend (Server)**:
  * Authoritative game rules and turn enforcement.
  * Move validation (bounds 1–9, unoccupied cells).
  * Board state consistency validation (equal symbol count before player move).
  * Winner and draw detection across all 8 winning lines.
  * Computer decision algorithms (Easy and Medium).

---

## 4. Stateless Design

A core architectural principle of this system is **pure statelessness**:

* **No HTTP Sessions** (`HttpSession` is not used).
* **No Database or ORM** (no JPA, Hibernate, SQLite, or H2).
* **No Server-Side In-Memory Cache** (no static board variables, no Redis, no session maps).

Each turn is processed as an isolated request:
1. Client submits: `{ "board": [...], "move": 5, "difficulty": "MEDIUM" }`.
2. Backend validates board and move legality.
3. Backend applies player move `X`, checks for Player Win / Draw.
4. If still `IN_PROGRESS`, backend computes Computer move `O`, checks for Computer Win / Draw.
5. Backend returns the updated board, resulting status, and computer move position.

---

## 5. Computer AI Strategies

### Easy AI
* Queries the board for all available empty positions (`""`).
* Selects one cell uniformly at random using `java.util.Random`.
* Does **not** inspect lines, evaluate wins, or attempt to block.

### Medium AI
Implements a strict 3-tier rule-based hierarchy:

```text
Step 1: CAN COMPUTER WIN?
   ├── YES ──> Play winning cell (Priority 1: WIN)
   └── NO
        ↓
Step 2: CAN PLAYER WIN NEXT TURN?
   ├── YES ──> Play blocking cell (Priority 2: BLOCK)
   └── NO
        ↓
Step 3: FALLBACK
   └── Select random available cell (Priority 3: RANDOM)
```

> **Critical Priority Guarantee**: If both the computer and the player threaten an immediate win on the board, the computer **always prioritizes taking its own winning move** rather than blocking.

---

## 6. Primary API Endpoint

### `POST /api/game/move`

#### Request Payload
```json
{
  "board": [
    "", "", "",
    "", "", "",
    "", "", ""
  ],
  "move": 5,
  "difficulty": "MEDIUM"
}
```

#### Response Payload (`200 OK`)
```json
{
  "board": [
    "", "", "",
    "", "X", "",
    "", "", "O"
  ],
  "status": "IN_PROGRESS",
  "message": "Computer played position 9. Your turn!",
  "nextTurn": "PLAYER",
  "computerMove": 9
}
```

#### Error Payload (`400 Bad Request`)
```json
{
  "error": "INVALID_MOVE",
  "message": "Position 5 is already occupied by 'X'."
}
```

For complete endpoint contracts, status values, and validation error scenarios, see [API.md](API.md).

---

## 7. How to Run the Application

### Prerequisites
* **Java**: JDK 21+ (tested on Java 21 / OpenJDK 25)
* **Maven**: 3.9+ (or use local Maven installation)
* Modern web browser (Chrome, Edge, Firefox, Safari)

### Step 1: Start the Backend
Navigate to the `backend` directory and run:
```bash
cd backend
mvn spring-boot:run
```
The server will start on port `8080`.

### Step 2: Open the Frontend
You have two convenient options:

* **Option A (Integrated — Recommended)**:  
  Open your browser and navigate directly to:
  ```text
  http://localhost:8080/
  ```
  The Spring Boot backend automatically serves the frontend static assets.

* **Option B (Independent Frontend)**:  
  Open `frontend/index.html` directly in your browser or serve it with any static web server (e.g. VS Code Live Server on `http://127.0.0.1:5500`). The frontend detects its hosting origin and automatically communicates with `http://localhost:8080/api/game/move` via pre-configured CORS headers.

---

## 8. How to Run Automated Tests

To execute the full automated test suite (122 unit and integration tests):

```bash
cd backend
mvn clean test
```

### Test Coverage Highlights
* `GameEngineTest` (30 tests): Validates all 8 winning rows/columns/diagonals, draw conditions, winning move simulation, and board/move boundary rules.
* `ComputerPlayerTest` (10 tests): Validates Easy random selection, Medium scenarios (A: Win, B: Block, C: Random fallback, D: Priority Win over Block), and Hard strategy delegation (winning move, blocking move).
* `EasyStrategyTest` (5 tests): Validates uniform random distribution, empty board moves, terminal/full board exceptions, and sole remaining empty cell picks.
* `MediumStrategyTest` (5 tests): Tests winning move priority, player blocking priority, win over block prioritization, and empty cell fallbacks.
* `HardStrategyTest` (13 tests): Tests unbeatable Minimax with alpha-beta pruning, immediate win, immediate block, fork prevention, blunder exploitation, deterministic tie-breaking, alpha-beta node reduction, 100-game Monte Carlo simulation, and all 9 opening responses.
* `DomainModelTest` (4 tests): Tests `GameMode`, `PlayerSymbol`, `Difficulty`, and `GameStatus` enums and terminal state helpers.
* `GameServiceTest` (10 tests): Validates turn progression, immediate player victory termination, draw detection on 9th move, illegal move rejections, and optional persistence delegation.
* `GameControllerTest` (17 tests): Full-stack `MockMvc` testing covering endpoint routing, JSON aliasing (`move`/`position`), game outcome payloads, and successful 200 OK processing of `difficulty: "HARD"`.
* `PlayerProfileRepositoryTest` (3 tests): Verifies profile persistence, unique username enforcement, and case-insensitive lookups.
* `GameRecordRepositoryTest` (1 test): Verifies paginated user match history and proper game-participant relationships.
* `GameParticipantRepositoryTest` (1 test): Verifies aggregate player statistics across wins, losses, draws, and zero-game players.
* `PlayerProfileServiceTest` (5 tests): Validates username formatting rules, duplicate rejection, and profile retrieval.
* `GameHistoryServiceTest` (6 tests): Validates authoritative match persistence, guest omission, terminal outcome mapping, and duration calculations.
* `LeaderboardServiceTest` (2 tests): Validates leaderboard ranking sort order, win rate formulas, and safe zero-game handling.
* `UserControllerTest` (6 tests): Tests `/api/users` endpoints for creation (201), conflict (409), validation (400), lookup (200/404), and history pagination.
* `LeaderboardControllerTest` (1 test): Tests `/api/leaderboard` ranking retrieval.
* `GamePersistenceIntegrationTest` (2 tests): End-to-end integration verifying guest zero-persistence and registered user persistence reflecting on history and leaderboards.
* `TicTacToeApplicationTests` (1 test): Verifies Spring application context loading and static welcome page mapping.

---

## 9. MVP2 Evolution & Roadmap

The project is currently evolving toward **MVP2** in structured phases:

- [x] **Phase 1: Architecture & Data Model Foundation** (Completed)
  - Strategy pattern refactoring for AI difficulties (`MoveStrategy`, `EasyStrategy`, `MediumStrategy`, `HardStrategy`).
  - Domain model enhancements (`GameMode`, `PlayerSymbol`, `Difficulty.HARD`, `GameStatus` lifecycle).
  - Target architecture diagrams, persistence schema designs, STOMP contracts, and ADRs documented in [docs/MVP2_PHASE1.md](file:///c:/Users/User/Desktop/homework/Tic%20Tac%20Toe/docs/MVP2_PHASE1.md).
  - 100% backward compatibility preserved for MVP1 REST API, board rules, and frontend.
- [x] **Phase 2: Hard Difficulty AI** (Completed)
  - Unbeatable computer opponent powered by the Minimax algorithm with alpha-beta pruning.
  - Depth-sensitive heuristic scoring (`+10 - depth`, `depth - 10`, `0`).
  - Board state immutability via `Board.withMove(...)`.
  - Comprehensive documentation in [docs/MVP2_PHASE2.md](file:///c:/Users/User/Desktop/homework/Tic%20Tac%20Toe/docs/MVP2_PHASE2.md).
- [x] **Phase 3: Database, User Profiles, Persistent History & Leaderboards** (Completed)
  - Spring Data JPA, PostgreSQL production setup, and embedded H2 development/testing mode.
  - Minimal user profiles (`PlayerProfile`) with unique username constraints.
  - Server-authoritative game persistence (`GameRecord`, `GameParticipant`) for completed matches.
  - Dynamic leaderboard calculations directly from persistent match records with safe win-rate handling.
  - Comprehensive documentation in [docs/MVP2_PHASE3.md](file:///c:/Users/User/Desktop/homework/Tic%20Tac%20Toe/docs/MVP2_PHASE3.md).
- [ ] **Phase 4: Online Real-Time Multiplayer** (Next Phase)
  - Spring WebSocket with STOMP protocol, matchmaking queue, game rooms, and player disconnect handling.
- [ ] **Phase 5: Modern Responsive Web UI**
  - Professional redesign with navigation bar, game areas, leaderboards, match history, and profile screens.


