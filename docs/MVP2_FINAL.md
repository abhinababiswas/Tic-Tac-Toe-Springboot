# Tic-Tac-Toe MVP2 — Final Architectural & Technical Specification

## 1. Project Overview
Tic-Tac-Toe MVP2 is a full-featured, server-authoritative web platform that combines classical game theory, real-time messaging, and relational database persistence into a responsive gaming website. Users can challenge an intelligent computer opponent across three distinct difficulty modes (including an unbeatable Minimax AI with alpha-beta pruning) or compete head-to-head in real time with another player online through WebSocket and STOMP protocols.

---

## 2. Complete MVP2 Feature Inventory

| Feature Domain | Capability | Description |
| :--- | :--- | :--- |
| **Game Engine** | 3x3 Grid Rules | Centralized winner detection (8 lines), draw detection, and move validation. |
| | Immutability | Functional `Board.withMove(index, symbol)` returns a new instance, preventing state mutation. |
| | Server-Authoritative | 100% of game progression, legal moves, and terminal states are calculated by Spring Boot. |
| **AI Strategies** | Easy Difficulty | Uniformly random selection among available empty cells. |
| | Medium Difficulty | Tactical tree prioritizing **Immediate Win > Immediate Block > Random Selection**. |
| | Hard Difficulty | Unbeatable Minimax algorithm with alpha-beta pruning and depth-penalized evaluation. |
| **User Management** | Player Profiles | Unique display usernames, persistent IDs, and guest mode support. |
| | Identity Tracking | Anonymous guest session auto-registration for multiplayer persistence. |
| **Persistence** | Database Layer | Spring Data JPA with PostgreSQL compatibility and in-memory H2 development support. |
| | Game Records | Full match tracking (`GameRecord`, `GameParticipant`) for single-player and multiplayer. |
| | Leaderboard | Real-time SQL aggregation computing games played, wins, losses, draws, and win rates. |
| | Match History | Paginated match history with outcome badges, opponent info, and completion dates. |
| **Multiplayer** | WebSocket + STOMP | Real-time duplex channel over native WebSocket and SockJS fallback (`/ws`). |
| | Matchmaking | In-memory FIFO queue with duplicate join protection and disconnect cleanup. |
| | Concurrency Control| Instance monitor locks (`synchronized applyMove`) rejecting simultaneous or stale moves. |
| | Disconnect Policy | Active match participants who drop trigger an automatic `ABANDONED` transition. |
| **Frontend Web UI**| Responsive Website | Single-page architecture with hash-based view router (`#home`, `#play`, `#multiplayer`, `#leaderboard`, `#history`). |
| | Modern Design System| Dark gaming theme (`#0a0e17`), electric cyan/rose accents, glassmorphic filters, micro-animations. |
| | Accessibility | Semantic HTML5, full keyboard navigation, visible focus rings, and ARIA live regions. |

---

## 3. Technology Stack

### Backend
- **Java 21**: Core domain classes, records, concurrency controls, and strategy algorithms.
- **Spring Boot 3.4.3**:
  - `spring-boot-starter-web`: REST controllers, global exception handling, content negotiation.
  - `spring-boot-starter-websocket`: STOMP message broker, handshake handlers, event listeners.
  - `spring-boot-starter-data-jpa`: Hibernate ORM, repository abstractions, transactional boundaries.
  - `spring-boot-starter-validation`: Jakarta bean validation annotations.
- **Database**:
  - **H2 Database**: Zero-configuration in-memory database with PostgreSQL dialect emulation.
  - **PostgreSQL**: Production relational database driver.
- **Build & Test**: Maven 3.9+ with Surefire runner and JUnit 5 / MockMvc testing framework.

### Frontend
- **HTML5**: Semantic tags (`<header>`, `<nav>`, `<main>`, `<section>`, `<footer>`), ARIA live regions.
- **Vanilla CSS3**: CSS custom property tokens, flexbox/grid layout systems, glassmorphism (`backdrop-filter`).
- **Vanilla JavaScript (ES6+)**:
  - `api.js`: REST client module.
  - `socket.js`: SockJS + STOMP WebSocket client manager.
  - `app.js`: State manager, view router, and DOM event coordinator.
- **Libraries**: SockJS Client (1.6.1) and STOMP.js (2.3.3) via CDN.

---

## 4. End-to-End System Architecture

```
                          Browser (User Interface)
                                     |
             +-----------------------+-----------------------+
             |                                               |
       HTTP REST API                                   WebSocket STOMP
   (/api/game/move, etc.)                                  (/ws)
             |                                               |
             v                                               v
      GameController                             MultiplayerWebSocketController
      UserController                                         |
    LeaderboardController                                    v
             |                                      MultiplayerService
             v                                      +--------+--------+
        GameService                                 |                 |
             |                              MatchmakingService  GameSessions
             +-----------------------+--------------+                 |
                                     |                                |
                                     v                                v
                                GameEngine <--------------------------+
                                     |
                         +-----------+-----------+
                         |           |           |
                        Easy       Medium      Hard
                                                 |
                                              Minimax
                                            (Alpha-Beta)
                                     |
                                     v
                           Spring Data JPA Layer
                                     |
                                     v
                        Relational Database (H2/Postgres)
```

---

## 5. Security & Threat Modeling
- **Server Authority**: The client has zero authority over game outcomes, board positions, or turn counts. Every move is submitted to the backend which calculates win, draw, or continuation.
- **Input Validation**: Positions strictly checked (`1 <= position <= 9`), empty cell verification, and active status checks.
- **Concurrency Protection**: `synchronized applyMove(...)` on each active game session guarantees atomic turn advancement, preventing race conditions or double-moves.
- **Data Protection**: Passwords and secrets are never committed or logged. User inputs are sanitized with HTML entity escaping (`escapeHtml()`) in the frontend to prevent XSS.
- **CORS Isolation**: Explicitly allows only trusted local development origins (`localhost:3000`, `localhost:5500`, `localhost:8080`) rather than open wildcards in REST controllers.

---

## 6. Testing & Quality Assurance
- **Total Test Cases**: **147 tests** across 15 test classes.
- **Pass Rate**: **100% (0 failures, 0 errors, 0 skipped)**.
- **Build Health**: Maven compile, test, and production packaging (`mvn clean package`) all succeed cleanly.

---

## 7. Known Scalability Limitations
1. **In-Memory Matchmaking & Sessions**: Matchmaking queue and active multiplayer game sessions reside in application memory. In a multi-instance clustered environment, a distributed cache or external message broker (e.g., Redis, RabbitMQ) would be required to coordinate across instances.
2. **Simplified Identity**: The current identity model uses persistent display usernames. Full production deployment would benefit from password hashing (BCrypt) and JWT/session tokens.

---

## 8. Deployment Requirements
- **Runtime**: Java 21 JRE or JDK.
- **Container**: Pre-configured `Dockerfile` with multi-stage build (Maven compile + Eclipse Temurin 21 JRE runtime).
- **Environment Variables**:
  - `PORT`: HTTP port (defaults to 8080).
  - `SPRING_DATASOURCE_URL`: JDBC database URL.
  - `SPRING_DATASOURCE_USERNAME`: Database username.
  - `SPRING_DATASOURCE_PASSWORD`: Database password.
