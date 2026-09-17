# Tic-Tac-Toe MVP2 — Complete API & Protocol Specification

## 1. Overview

The Tic-Tac-Toe MVP2 platform exposes two complementary communication channels:
1. **Stateless REST API** (Port 8080): Handles Player vs Computer moves, user profile management, global leaderboards, and personal game history.
2. **Real-Time WebSocket & STOMP Broker** (`/ws`): Handles real-time online matchmaking, two-player sessions, and live move synchronization.

**Base URL**: `http://localhost:8080`  
**Content-Type**: `application/json`

---

## 2. REST API Endpoints

### 2.1 Make Computer Move
`POST /api/game/move`

Processes a human move against the server-side computer AI and returns the resulting board, status, and computer move.

#### Request Headers
| Header | Value | Description |
| :--- | :--- | :--- |
| `Content-Type` | `application/json` | Required JSON payload |

#### Request Body
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `board` | `List<String>` | Yes | Array of 9 strings representing cells 1–9. Valid values: `"X"`, `"O"`, `""`. |
| `move` (or `position`) | `Integer` | Yes | 1-based board position (`1` to `9`) where player places `"X"`. |
| `difficulty` | `String` | Optional (default `"EASY"`) | AI difficulty: `"EASY"`, `"MEDIUM"`, or `"HARD"`. |
| `userId` | `Long` | Optional | Persistent user ID. If provided, completed matches are saved to history. |
| `startedAt` | `String` (ISO 8601) | Optional | Client timestamp when the match began for duration tracking. |

#### Example Request
```json
{
  "board": ["", "", "", "", "", "", "", "", ""],
  "move": 5,
  "difficulty": "HARD",
  "userId": 42,
  "startedAt": "2026-09-18T04:00:00.000Z"
}
```

#### Example Response (HTTP 200 OK)
```json
{
  "board": ["", "", "", "", "X", "", "", "", "O"],
  "status": "IN_PROGRESS",
  "message": "Computer played position 9. Your turn!",
  "nextTurn": "PLAYER",
  "computerMove": 9,
  "winningLine": null
}
```

---

### 2.2 Create User Profile
`POST /api/users`

Registers a new user profile with a unique username.

#### Request Body
```json
{
  "username": "Neo"
}
```

#### Response (HTTP 201 Created)
```json
{
  "id": 1,
  "username": "Neo",
  "createdAt": "2026-09-18T04:00:00.000Z"
}
```

#### Conflict (HTTP 409 Conflict)
Returned if the username is already taken.
```json
{
  "error": "USERNAME_ALREADY_EXISTS",
  "message": "Username 'Neo' is already taken."
}
```

---

### 2.3 Lookup User by Username
`GET /api/users/by-username/{username}`

Retrieves an existing player profile by display username.

#### Response (HTTP 200 OK)
```json
{
  "id": 1,
  "username": "Neo",
  "createdAt": "2026-09-18T04:00:00.000Z"
}
```

#### Not Found (HTTP 404 Not Found)
```json
{
  "error": "USER_NOT_FOUND",
  "message": "User not found with username: Neo"
}
```

---

### 2.4 User Match History
`GET /api/users/{userId}/history?page=0&size=10`

Retrieves a paginated list of completed single-player and multiplayer matches for the given user.

#### Response (HTTP 200 OK)
```json
{
  "content": [
    {
      "gameId": 101,
      "gameMode": "COMPUTER",
      "opponent": "Computer",
      "difficulty": "HARD",
      "outcome": "WIN",
      "completedAt": "2026-09-18T04:05:00.000Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 2.5 Global Leaderboard
`GET /api/leaderboard?limit=10`

Returns the top ranked players ordered by total wins (descending) and win rate.

#### Response (HTTP 200 OK)
```json
[
  {
    "rank": 1,
    "userId": 1,
    "username": "Neo",
    "gamesPlayed": 12,
    "wins": 10,
    "losses": 1,
    "draws": 1,
    "winRate": 83.33
  }
]
```

---

## 3. WebSocket & STOMP Protocol

### 3.1 Connection Handshake
- **WebSocket Endpoint**: `/ws`
- **Supported Transports**: Native WebSocket (`ws://localhost:8080/ws`) and SockJS fallback (`http://localhost:8080/ws`).
- **Authentication/Session**: Anonymous handshake automatically assigns a unique UUID `Principal` to route private user responses.

---

### 3.2 Client Destinations (`/app`)

#### Join Matchmaking Queue
- **Destination**: `/app/matchmaking/join`
- **Payload**:
  ```json
  {
    "userId": 1,
    "username": "Neo"
  }
  ```

#### Leave Matchmaking Queue
- **Destination**: `/app/matchmaking/leave`
- **Payload**:
  ```json
  {
    "userId": 1
  }
  ```

#### Submit Multiplayer Move
- **Destination**: `/app/game/{gameId}/move`
- **Payload**:
  ```json
  {
    "position": 5,
    "userId": 1
  }
  ```

---

### 3.3 Server Destinations (`/user` and `/topic`)

#### Private Match Found Notification
- **Destination**: `/user/queue/match` (Fallback: `/topic/match/{userId}`)
- **Payload (`MatchFoundMessage`)**:
  ```json
  {
    "gameId": "7fe0e18d-3b55-4a96-8bd2-b0771921be76",
    "yourSymbol": "X",
    "opponentUsername": "Trinity",
    "currentTurn": "X",
    "board": ["", "", "", "", "", "", "", "", ""],
    "status": "IN_PROGRESS"
  }
  ```

#### Public Game Session Broadcast
- **Destination**: `/topic/game/{gameId}`
- **Payload (`GameStateMessage`)**:
  ```json
  {
    "type": "GAME_UPDATE",
    "gameId": "7fe0e18d-3b55-4a96-8bd2-b0771921be76",
    "board": ["", "", "", "", "X", "", "", "", ""],
    "currentTurn": "O",
    "status": "IN_PROGRESS",
    "message": "Player X moved to position 5.",
    "lastMovePosition": 5,
    "lastMovePlayer": "Neo",
    "winner": null,
    "winningLine": null
  }
  ```

#### Private Error Notifications
- **Destination**: `/user/queue/errors`
- **Payload (`WebSocketErrorMessage`)**:
  ```json
  {
    "errorCode": "NOT_YOUR_TURN",
    "errorMessage": "It is not your turn.",
    "timestamp": 1726617600000
  }
  ```
