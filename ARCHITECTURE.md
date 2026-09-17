# Tic-Tac-Toe MVP 1 — Architecture & Design Document

## 1. Project Purpose & Scope

The purpose of Tic-Tac-Toe MVP 1 is to provide a clean, responsive, web-based Tic-Tac-Toe game where a human player plays against the computer.

### In Scope for MVP 1
- Single-player game: Human Player (`X`) vs Computer (`O`).
- Two AI difficulty modes:
  - **Easy**: Selects a random available position.
  - **Medium**: Rule-based strategy (1. Win if possible; 2. Block player if player threatens a win; 3. Random available position otherwise).
- Session score tracking in the browser (Player Wins, Computer Wins, Draws).
- Responsive, clean UI (HTML5, CSS3, Vanilla JavaScript).
- Stateless Spring Boot backend validating moves, applying rules, and executing computer turns.

### Explicitly Out of Scope for MVP 1
- Databases / persistence (no JPA, Hibernate, PostgreSQL, H2, SQLite, MongoDB).
- User authentication, authorization, accounts, or profiles.
- Server-side game sessions, HTTP sessions, or Redis.
- WebSockets, long polling, or push notifications.
- Microservices, message queues (Kafka, RabbitMQ), or cloud infrastructure.
- Hard difficulty / Minimax algorithm / AI machine learning frameworks.
- Multiplayer / matchmaking / leaderboards.
- Frontend frameworks (React, Vue, Angular, Tailwind, Bootstrap, TypeScript).

---

## 2. Technology Stack

- **Backend**:
  - Language: Java 21 (running on OpenJDK 25 environment with `--release 21` compilation)
  - Framework: Spring Boot 3.4.3
  - Build Tool: Apache Maven 3.9+
  - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-test` (no database, no security starters)
- **Frontend**:
  - HTML5 (semantic structure, accessibility attributes)
  - CSS3 (Vanilla CSS, CSS custom properties, grid layout)
  - JavaScript (Vanilla ES6+, fetch API)

---

## 3. Separation of Responsibilities

### Frontend Responsibilities
- Rendering the 3x3 game board and updating cell states.
- Capturing user input (cell clicks, difficulty changes, restart).
- Maintaining the user-facing score counter for the current browser session.
- Submitting the player's move along with the current board state and selected difficulty to the backend.
- Rendering authoritative game state and status messages returned from the backend.
- **The frontend is never the authoritative source of game rules or validation.**

### Backend Responsibilities
- Validating the incoming request:
  - Board state validity (exact 9 cells, valid symbols).
  - Move position validity (range 1–9, targeting an unoccupied cell).
  - Game progression validity (rejecting moves if the game has already concluded).
- Applying game rules authoritatively:
  - Applying player's move to the board.
  - Checking for player win or draw.
  - If game is still in progress, computing computer move according to difficulty strategy.
  - Applying computer move and checking for computer win or draw.
- Returning the resulting board and game status.

---

## 4. Stateless Backend Architecture

The backend maintains **no server-side state**:
- No in-memory game session maps or repositories.
- No HTTP sessions (`HttpSession`).
- No database or Redis.

Each move is an atomic HTTP request containing the current state needed to calculate the next state:

```text
Browser (Client State)
   |
   | POST /api/game/move
   | Payload: { "board": [...], "position": 5, "difficulty": "MEDIUM" }
   |
   v
Spring Boot Backend (Stateless Execution)
   |
   | 1. Validate payload & move legality
   | 2. Apply player move ('X')
   | 3. Check for Player Win / Draw
   | 4. If IN_PROGRESS, calculate & apply Computer move ('O')
   | 5. Check for Computer Win / Draw
   |
   v
Response Payload: { "board": [...], "status": "IN_PROGRESS", "message": "..." }
   |
   v
Browser (Updates Display & Session Score)
```

---

## 5. Board Representation

The board is logically represented as a flat list/array of 9 elements containing `"X"`, `"O"`, or `""` (empty):

```text
Logical Grid:
 1 | 2 | 3
---+---+---
 4 | 5 | 6
---+---+---
 7 | 8 | 9

Array Index Mapping (0-based):
Index 0 -> Position 1
Index 1 -> Position 2
Index 2 -> Position 3
Index 3 -> Position 4
Index 4 -> Position 5
Index 5 -> Position 6
Index 6 -> Position 7
Index 7 -> Position 8
Index 8 -> Position 9
```

This representation is:
- Easy to serialize to/from JSON (`["X", "", "O", "", "X", "", "", "", ""]`).
- Easily consumed by both Java (`List<String>`) and JavaScript (`Array`).
- Independent of Java-specific constructs like `char[][]`.

---

## 6. Finite Game States

Defined by the `GameStatus` enum:
- `IN_PROGRESS`: Game is active; neither player has won and empty cells remain.
- `PLAYER_WON`: Player (`X`) has achieved 3 in a row.
- `COMPUTER_WON`: Computer (`O`) has achieved 3 in a row.
- `DRAW`: All 9 cells are occupied with no 3-in-a-row winner.

---

## 7. AI Difficulty Strategies

Defined by the `Difficulty` enum:

### Easy
- Scans the board for all available empty positions (`""`).
- Selects one uniformly at random.
- No strategic evaluation.

### Medium
Rule-based decision tree derived from the console prototype:
1. **Check for Win**: If the computer can win in one move, make that winning move.
2. **Check for Block**: If the player can win on their next move, choose the blocking move.
3. **Fallback to Random**: Otherwise, pick an empty position at random.

```text
Computer Turn Decision Tree (Medium):
               [Can computer win?]
                 /            \
              (Yes)           (No)
              /                  \
   [Play winning move]     [Can player win?]
                             /          \
                          (Yes)         (No)
                          /                \
                [Play blocking move]   [Pick random cell]
```

---

## 8. Mapping the Console Prototype to Backend Layers

The existing Java console prototype relies on `Scanner`, `Random`, console printing, and a 3x3 `char[][]`. The architecture cleanly separates these concerns:

| Console Prototype Element | Target Architecture Component | Role / Transformation |
| :--- | :--- | :--- |
| `displayBoard` | Frontend (`render()` in `app.js`) | Board visualization rendered in browser DOM from API response. |
| `playerMove` + `Scanner` | Frontend click -> HTTP Controller | Player selects cell via browser click, submitted via JSON DTO. |
| `placeMove` | `Board` / `GameEngine` | Pure board update transitioning cell state from `""` to `"X"` or `"O"`. |
| `checkWinner` | `GameEngine` | Evaluates all 8 winning lines (3 rows, 3 columns, 2 diagonals). |
| `checkDraw` | `GameEngine` | Verifies if all 9 positions are non-empty without a winner. |
| `isWinningMove` | `GameEngine` | Pure simulation checking if placing a symbol results in a win. |
| `findWinningMove` | `ComputerPlayer` | Iterates open cells using `isWinningMove` for win/block logic. |
| `computerMove` | `ComputerPlayer` | Implements Easy and Medium strategy without console I/O. |

---

## 9. Project & Package Structure

```text
Tic Tac Toe/
├── README.md                        # Primary developer guide, setup, and features
├── ARCHITECTURE.md                  # System architecture, stateless design, and domain model
├── API.md                           # Comprehensive REST API specifications and contracts
├── backend/
│   ├── pom.xml                      # Maven project configuration (Spring Boot 3.4.3, Java 21)
│   └── src/
│       ├── main/
│       │   ├── java/com/tictactoe/
│       │   │   ├── TicTacToeApplication.java    # Spring Boot application entry point
│       │   │   ├── controller/                  # REST API boundary
│       │   │   │   ├── GameController.java      # /api/game/move and /health endpoints
│       │   │   │   └── package-info.java
│       │   │   ├── service/                     # Turn orchestration layer
│       │   │   │   ├── GameService.java         # Stateless game turn coordinator
│       │   │   │   └── package-info.java
│       │   │   ├── engine/                      # Core domain rules & AI decision-making
│       │   │   │   ├── GameEngine.java          # Win/draw detection & move validation
│       │   │   │   ├── ComputerPlayer.java      # Easy (random) & Medium (win>block>random)
│       │   │   │   └── package-info.java
│       │   │   ├── model/                       # Immutable models & DTOs
│       │   │   │   ├── Board.java               # Immutable 9-cell board abstraction
│       │   │   │   ├── MoveRequest.java         # Request payload DTO
│       │   │   │   ├── GameResponse.java        # Authoritative response payload DTO
│       │   │   │   ├── ErrorResponse.java       # Standardized error payload DTO
│       │   │   │   ├── GameStatus.java          # IN_PROGRESS, PLAYER_WON, COMPUTER_WON, DRAW
│       │   │   │   ├── Difficulty.java          # EASY, MEDIUM
│       │   │   │   └── package-info.java
│       │   │   └── exception/                   # Exception handling
│       │   │       ├── GlobalExceptionHandler.java # @RestControllerAdvice for clean JSON errors
│       │   │       ├── InvalidBoardException.java
│       │   │       ├── InvalidMoveException.java
│       │   │       └── package-info.java
│       │   └── resources/
│       │       ├── application.properties       # Server port & application properties
│       │       └── static/                      # Bundled frontend assets served by Spring Boot
│       │           ├── index.html
│       │           ├── css/style.css
│       │           └── js/app.js
│       └── test/
│           └── java/com/tictactoe/
│               ├── TicTacToeApplicationTests.java # Context loading test
│               ├── controller/
│               │   └── GameControllerTest.java    # MockMvc full-stack API tests (16 tests)
│               ├── service/
│               │   └── GameServiceTest.java       # Turn orchestration unit tests (9 tests)
│               └── engine/
│                   ├── GameEngineTest.java        # Rule & validation unit tests (30 tests)
│                   └── ComputerPlayerTest.java    # AI strategy unit tests (8 tests)
└── frontend/                                    # Standalone frontend distribution
    ├── index.html                               # Semantic markup, board grid, controls, score display
    ├── css/
    │   └── style.css                            # Styling, responsive layout, dark theme
    └── js/
        └── app.js                               # Client-side UI handling & state management
```

---

## 10. Production API Contract

- **Endpoint**: `POST /api/game/move`
- **Request Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "board": ["X", "", "", "", "O", "", "", "", ""],
  "move": 3,
  "difficulty": "MEDIUM"
}
```
- **Success Response (200 OK)**:
```json
{
  "board": ["X", "", "X", "", "O", "", "O", "", ""],
  "status": "IN_PROGRESS",
  "message": "Computer played position 7. Your turn!",
  "nextTurn": "PLAYER",
  "computerMove": 7
}
```
- **Error Response (400 Bad Request)**:
```json
{
  "error": "INVALID_MOVE",
  "message": "Position 3 is already occupied by 'X'."
}
```

---

## 11. Automated Test Architecture

The system achieves comprehensive test coverage (64 automated tests) across all layers without requiring external mock libraries or databases:
- **Domain Engine Tests (`GameEngineTest`)**: Exhaustive parameterized tests for all 8 winning rows, columns, and diagonals; draw detection; winning move simulation; and boundary checks.
- **AI Strategy Tests (`ComputerPlayerTest`)**: Deterministic testing with injected `Random` seeds, verifying Easy uniform distribution, Medium Scenario A (Win), Scenario B (Block), Scenario C (Random), and Scenario D (Strict Win > Block prioritization).
- **Service Orchestration Tests (`GameServiceTest`)**: Verifies that human winning moves immediately terminate the turn without triggering a computer response, that 9th-move draws correctly resolve, and that illegal moves throw expected domain exceptions.
- **API Integration Tests (`GameControllerTest`)**: Full-stack Spring MVC tests using `MockMvc` verifying HTTP status codes, JSON serialization/deserialization, alias mapping (`move`/`position`), and clean error payloads free from stack traces.

---

## 12. Evolution to MVP2 (Phase 1 Architecture Foundation)

Phase 1 established the architecture and data model foundation for MVP2:
- Strategy pattern for AI difficulties (`MoveStrategy`, `EasyStrategy`, `MediumStrategy`, `HardStrategy`).
- Domain model expansion (`GameMode`, `PlayerSymbol`, `Difficulty`, `GameStatus`).
- Detailed specifications, data schemas, STOMP contracts, and ADRs: [docs/MVP2_PHASE1.md](file:///c:/Users/User/Desktop/homework/Tic%20Tac%20Toe/docs/MVP2_PHASE1.md).

---

## 13. Hard AI Architecture (Phase 2: Minimax + Alpha-Beta Pruning)

Phase 2 implements the unbeatable `HardStrategy` using the Minimax algorithm:

### 13.1 Strategy Specification
- **Maximizing Player**: Computer (`O`).
- **Minimizing Player**: Human Player (`X`).
- **Scoring Function**:
  - Computer Win: `+10 - depth` (promotes earliest win).
  - Human Win: `depth - 10` (delays inevitable loss).
  - Draw: `0`.
- **Alpha-Beta Pruning**: Prunes subtrees where `beta <= alpha`, reducing evaluations by ~96% on opening moves.
- **Deterministic Tie-Breaking**: Scans moves in index order 0..8, choosing the first move with maximum score.
- **State Immutability**: Uses pure `Board.withMove(...)` functional transformations to prevent board corruption.
- **Documentation**: Comprehensive analysis and metrics available in [docs/MVP2_PHASE2.md](file:///c:/Users/User/Desktop/homework/Tic%20Tac%20Toe/docs/MVP2_PHASE2.md).

---

## 14. Persistence & Player Statistics (Phase 3: JPA, History & Leaderboards)

Phase 3 introduces relational database persistence using Spring Data JPA while keeping the core game engine pure and decoupled:

### 14.1 Database Architecture
- **Production Database**: PostgreSQL (`postgresql` driver).
- **Development & Test Database**: Embedded H2 (`MODE=PostgreSQL`).
- **Entity Model**:
  - `PlayerProfile`: Represents registered human users (`id`, `username`, `createdAt`, `updatedAt`). Unique username constraints enforced at both JPA and database levels.
  - `GameRecord`: Stores server-authoritative completed matches (`gameMode`, `difficulty`, `status`, `result`, `startedAt`, `completedAt`).
  - `GameParticipant`: Links players to game records (`symbol`, `participantType`, `outcome`). Supports single-player (computer opponent represented with `player = null`) and future multiplayer without schema migration.
- **Strict Decoupling**: Core `GameEngine`, `Board`, and AI strategies contain zero JPA annotations or database dependencies.
- **Server Authority**: Persistence occurs exclusively through the service layer after the server determines a terminal game outcome.
- **Guest vs. Registered Gameplay**: Anonymous guest play remains fully supported with zero database writes. Registered moves optionally specify `userId`, automatically persisting terminal games.
- **Leaderboard Calculation**: Real-time aggregate queries calculating games played, wins, losses, draws, and win rates directly from completed game records.
- **Detailed Documentation**: See [docs/MVP2_PHASE3.md](file:///c:/Users/User/Desktop/homework/Tic%20Tac%20Toe/docs/MVP2_PHASE3.md).

---

## 15. Real-Time Online Multiplayer Architecture (Phase 4: WebSocket + STOMP)

Phase 4 adds real-time Player vs Player online multiplayer with server-authoritative matchmaking, session management, and move synchronization:

### 15.1 Architectural Separation
- **REST Computer Gameplay**: Unmodified REST API (`/api/game/move`) for Easy, Medium, and Hard (Minimax) play.
- **WebSocket STOMP Multiplayer**: Event-driven real-time messaging layer (`/ws`, `/app`, `/topic`, `/user/queue`).

### 15.2 WebSocket & STOMP Specifications
- **Endpoints**: `/ws` with native WebSocket and SockJS fallback transports.
- **Application Destination Prefix**: `/app`.
- **User Destination Prefix**: `/user`.
- **Simple Broker**: In-memory broker serving `/topic` and `/queue`.
- **Destinations**:
  - Client -> Server: `/app/matchmaking/join`, `/app/matchmaking/leave`, `/app/game/{gameId}/move`.
  - Server -> Client: `/user/queue/match` (match pairing and symbol assignment), `/user/queue/errors` (structured error payloads), `/topic/game/{gameId}` (public match state broadcasts).

### 15.3 Authoritative Session & Concurrency Control
- **In-Memory FIFO Matchmaking**: `MatchmakingService` pairs waiting players in strict FIFO order, prevents duplicate queue joins, and cleans up disconnected sessions.
- **Active Game Session**: `MultiplayerGameSession` maintains runtime state and serializes move processing via instance monitor locks (`synchronized applyMove(...)`), eliminating race conditions on simultaneous or double-move requests.
- **GameEngine Reuse**: Moves are validated using `GameEngine.validateMove(...)` and applied via immutable `Board.withMove(...)` transformations.
- **Persistence & Statistics**: Terminal multiplayer games (`PLAYER_ONE_WON`, `PLAYER_TWO_WON`, `DRAW`) are saved to `GameRecord` (`GameMode.MULTIPLAYER`) and `GameParticipant`, automatically updating player profiles, history, and leaderboard rankings (+1 win, +1 loss, +1 draw).
- **Disconnect Handling**: If a player disconnects during an active game, the match is marked `ABANDONED` and the opponent is notified without unfair score distortion.
- **Detailed Documentation**: See [docs/MVP2_PHASE4.md](file:///c:/Users/User/Desktop/homework/Tic%20Tac%20Toe/docs/MVP2_PHASE4.md).



