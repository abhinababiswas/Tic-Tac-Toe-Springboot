# Tic-Tac-Toe MVP 1 — REST API Specification

## 1. Overview

The Tic-Tac-Toe backend provides a clean, stateless REST API. The client maintains the session state and submits the current 9-cell board along with the player's move. The backend validates the move, applies game rules, executes the computer's move, and returns the authoritative resulting game state.

**Base URL**: `http://localhost:8080`  
**Content-Type**: `application/json`

---

## 2. Primary Endpoint: Make Move

### `POST /api/game/move`

Processes a player's move and returns the resulting board and game status.

#### Request Headers
| Header | Value | Description |
| :--- | :--- | :--- |
| `Content-Type` | `application/json` | Required JSON request body |

#### Request Body Fields
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `board` | `List<String>` | Yes | Array of 9 strings representing cells 1–9. Valid values: `"X"`, `"O"`, `""`. |
| `move` (or `position`) | `Integer` | Yes | 1-based board position (`1` to `9`) where the player wants to place `"X"`. |
| `difficulty` | `String` | Optional (default `MEDIUM`) | AI difficulty level: `"EASY"` or `"MEDIUM"`. |

#### Request Example
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

---

## 3. Response Contracts

### Successful Turn — `IN_PROGRESS` (HTTP 200 OK)
Returned when both the player and computer have placed their marks, and the game is still underway.

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

### Player Victory — `PLAYER_WON` (HTTP 200 OK)
Returned when the player's move completes 3-in-a-row. The computer does not play.

```json
{
  "board": [
    "X", "X", "X",
    "O", "O", "",
    "", "", ""
  ],
  "status": "PLAYER_WON",
  "message": "Player wins!",
  "nextTurn": null,
  "computerMove": null
}
```

### Computer Victory — `COMPUTER_WON` (HTTP 200 OK)
Returned when the computer's subsequent move completes 3-in-a-row.

```json
{
  "board": [
    "O", "O", "O",
    "X", "X", "",
    "", "", ""
  ],
  "status": "COMPUTER_WON",
  "message": "Computer played position 3 and won!",
  "nextTurn": null,
  "computerMove": 3
}
```

### Draw — `DRAW` (HTTP 200 OK)
Returned when all 9 cells are occupied and neither party has won.

```json
{
  "board": [
    "X", "O", "X",
    "X", "O", "O",
    "O", "X", "X"
  ],
  "status": "DRAW",
  "message": "Game ended in a draw.",
  "nextTurn": null,
  "computerMove": null
}
```

---

## 4. Error Responses (HTTP 400 Bad Request)

Error payloads use a consistent JSON format:
```json
{
  "error": "<ERROR_CODE>",
  "message": "<DESCRIPTIVE_MESSAGE>"
}
```

### Error Scenarios

#### 1. Target Position Occupied
```json
{
  "error": "INVALID_MOVE",
  "message": "Position 1 is already occupied by 'X'."
}
```

#### 2. Position Out of Range
```json
{
  "error": "INVALID_MOVE",
  "message": "Move position 10 is invalid. Must be between 1 and 9."
}
```

#### 3. Attempting Move on Finished Game
```json
{
  "error": "INVALID_MOVE",
  "message": "Cannot make a move: Player has already won."
}
```

#### 4. Invalid Board Size
```json
{
  "error": "INVALID_BOARD",
  "message": "Board must contain exactly 9 cells."
}
```

#### 5. Inconsistent Turn Count
```json
{
  "error": "INVALID_BOARD",
  "message": "Invalid board state: expected equal count of 'X' and 'O' before player's turn, but found 2 'X' and 0 'O'."
}
```

#### 6. Invalid Board Symbol
```json
{
  "error": "INVALID_BOARD",
  "message": "Invalid symbol 'Z' at position 3. Only 'X', 'O', or empty are permitted."
}
```

#### 7. Malformed JSON / Invalid Enum
```json
{
  "error": "BAD_REQUEST",
  "message": "Malformed JSON request body or invalid enum value."
}
```

---

## 5. Difficulty Levels

| Difficulty | Decision Strategy |
| :--- | :--- |
| `EASY` | Selects uniformly at random among all available empty cells. No tactical heuristics. |
| `MEDIUM` | Rule-based priority tree:<br>1. **Win immediately**: If computer can win in 1 move, take it.<br>2. **Block player**: Else if player threatens an immediate win, take that cell.<br>3. **Random fallback**: Otherwise, select an available empty cell at random. |

---

## 6. Optional Utility Endpoint

### `GET /api/game/health`
Returns backend service availability.

#### Response (HTTP 200 OK)
```json
{
  "status": "UP",
  "service": "tictactoe-backend"
}
```

---

## 7. CORS Configuration

The endpoint is pre-configured with `@CrossOrigin` to support common local frontend development origins:
- `http://localhost:3000`
- `http://localhost:5500`
- `http://127.0.0.1:5500`
- `http://localhost:8080`
- `http://127.0.0.1:8080`
