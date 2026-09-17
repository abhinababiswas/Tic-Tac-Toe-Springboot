# Tic-Tac-Toe MVP2 — Phase 5: Modern Professional Website Redesign

## 1. Overview
Phase 5 transforms the Tic-Tac-Toe MVP2 frontend from a compact single-card demo into a full-scale, modern, responsive, and visually captivating gaming website.

---

## 2. Architectural Design

```
+-------------------------------------------------------------------------+
|                              NAVBAR                                     |
|  [Logo] Tic-Tac-Toe      Home | Play | Multiplayer | Leaderboard | History | [User Pill]  |
+-------------------------------------------------------------------------+
                                    |
                    Hash-based Client-side View Router
                                    |
  +---------------+---------------+-----------------+-------------------+
  |               |               |                 |                   |
  v               v               v                 v                   v
#view-home    #view-play   #view-multiplayer  #view-leaderboard   #view-history
(Hero & CTAs) (vs Computer)  (WebSocket STOMP)  (Global Rankings) (Player Feed)
                      |               |
                      v               v
                [TicTacToeApi]  [TicTacToeSocket]
                      |               |
                      v               v
                Spring REST      Spring WebSocket
                Port :8080       Port :8080 (/ws)
```

### Key Modules:
- **`frontend/index.html`**: Semantic HTML5 layout featuring a responsive sticky navbar, mobile hamburger drawer, five dedicated view containers, a user profile modal dialog, and an informative footer.
- **`frontend/css/style.css`**: Design system built on CSS custom properties, modern dark gaming palette with electric cyan (`#38bdf8`) and rose (`#f43f5e`) accents, glassmorphic blur filters (`backdrop-filter: blur(14px)`), smooth CSS micro-interactions, responsive grid/flexbox layouts, and `@media (prefers-reduced-motion: reduce)` accessibility support.
- **`frontend/js/api.js`**: Reusable REST API client for computer moves (`POST /api/game/move`), user profile creation/lookup (`/api/users`), leaderboards (`/api/leaderboard`), and paginated history (`/api/users/{id}/history`).
- **`frontend/js/socket.js`**: Robust WebSocket + STOMP connection manager handling duplex messaging over SockJS/WebSocket fallback, matchmaking queue subscriptions, and game session topic broadcasts.
- **`frontend/js/app.js`**: Application controller, hash-based view router, UI state coordinator, and board event dispatcher.

---

## 3. Design System & Tokens
- **Backgrounds**: `--bg-primary: #0a0e17`, `--bg-secondary: #111827`, `--surface-card: #1a2234`, `--surface-elevated: #242f46`.
- **Accents**:
  - Player X / Primary: `#38bdf8` (Electric Sky) with glow `rgba(56, 189, 248, 0.35)`.
  - Player O / Opponent / Computer: `#f43f5e` (Vibrant Rose) with glow `rgba(244, 63, 94, 0.35)`.
  - Victory / Online: `#10b981` (Emerald).
  - Draw / Warning: `#f59e0b` (Amber).
- **Typography**: Google Font `Outfit` (sans-serif) with system fallbacks.

---

## 4. Views & Features
1. **Home (`#home`)**:
   - Hero headline: *"Challenge the Board. Outplay the Opponent."*
   - Dynamic CTA action buttons (*Play vs AI*, *Play Online*, *Leaderboard*).
   - Mode overview cards (Single-Player with Easy/Medium/Hard Minimax; Multiplayer with real-time matchmaking).
   - Architecture highlights explaining server-authoritative logic, Minimax tree pruning, and WebSocket STOMP.
2. **Play vs Computer (`#play`)**:
   - Difficulty segmented selector with live strategy descriptions.
   - Player vs Computer scorecard tracking round wins and draws.
   - Responsive 3x3 interactive board with large touch targets, keyboard navigation, and winning line animation.
3. **Online Multiplayer (`#multiplayer`)**:
   - Matchmaking lobby with real-time status badge and animated radar search indicator.
   - Dual-player match info strip displaying assigned symbols and opponent username.
   - Server-authoritative move submission preventing duplicate turns or unauthorized clicks.
   - Disconnect and match abandonment handling with clean recovery.
4. **Leaderboard (`#leaderboard`)**:
   - Highlight stats bar (Total Players, Total Matches, Total Wins).
   - Ranked data table with gold, silver, bronze medals for top 3, win rates, and record breakdown.
5. **History (`#history`)**:
   - Personal match history feed with win/loss/draw outcome pills, timestamp, and match IDs.
   - Next/Previous page pagination controls.
6. **Profile Modal**:
   - Fast username registration or account switching.
   - One-click Guest mode.
   - Profile summary with User ID and active status.

---

## 5. Responsiveness & Accessibility
- Tested and optimized across mobile (320px, 375px), tablet (768px), and desktop (1024px, 1440px+).
- ARIA live status announcements (`aria-live="polite"`), grid/cell roles, and clear accessible labels (`aria-label`).
- Full keyboard support (tab navigation and Enter/Space activation).
