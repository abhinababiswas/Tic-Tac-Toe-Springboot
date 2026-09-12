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

To execute the full automated test suite (64 unit and integration tests):

```bash
cd backend
mvn clean test
```

### Test Coverage Highlights
* `GameEngineTest` (30 tests): Validates all 8 winning rows/columns/diagonals, draw conditions, winning move simulation, and board/move boundary rules.
* `ComputerPlayerTest` (8 tests): Validates Easy random selection, Medium scenarios (A: Win, B: Block, C: Random fallback, D: Priority Win over Block), and injectable deterministic randomness.
* `GameServiceTest` (9 tests): Validates turn progression, immediate player victory termination, draw detection on 9th move, and illegal move rejections.
* `GameControllerTest` (16 tests): Full-stack `MockMvc` testing covering endpoint routing, JSON aliasing (`move`/`position`), game outcome payloads, and all 400 Bad Request error handlers.
* `TicTacToeApplicationTests` (1 test): Verifies Spring application context loading and static welcome page mapping.

---

## 9. Deliberate Scope Boundaries (MVP 1)

To maintain architectural focus and high engineering quality, the following features are intentionally **excluded** from MVP 1:

* ❌ **Database / Persistence**: No database connection or saved match history across browser sessions.
* ❌ **User Authentication**: No user login, registration, or accounts.
* ❌ **Multiplayer / Matchmaking**: Human vs Human and online multiplayer are omitted.
* ❌ **Hard / Minimax AI**: Hard difficulty using full recursive game-tree search is reserved for a future iteration.
* ❌ **WebSockets**: Real-time duplex channels are unnecessary for a turn-based stateless REST game.

---

## 10. Future Roadmap

Potential future enhancements beyond MVP 1:
1. **Hard Difficulty**: Unbeatable computer opponent powered by the Minimax algorithm with alpha-beta pruning.
2. **Local 2-Player Mode**: Pass-and-play mode allowing two humans to play on the same screen.
3. **Persistent Leaderboards & History**: Spring Data JPA / PostgreSQL integration with user profiles and win/loss statistics.
4. **Online Multiplayer**: WebSocket-based matchmaking with STOMP protocol.
5. **Theme Customization**: User-selectable visual themes and sound effects.
