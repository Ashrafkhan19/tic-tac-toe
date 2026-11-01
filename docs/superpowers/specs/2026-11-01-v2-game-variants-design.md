# Tic Tac Toe v2: Expanded Gameplay Variants Design Spec

## Intent & Overview
In v1, player retention loops (Daily Challenge, XP, Streaks, Themes, and Statistics) were introduced on top of the classic 3×3 game. However, 3×3 Tic-Tac-Toe is mathematically solved and offers limited tactical depth for long-term engagement.

Tic Tac Toe v2 expands the game with new tactical board variants:
1. **Classic 3×3:** 3-in-a-row to win (unchanged).
2. **Grid 4×4:** 4-in-a-row to win.
3. **Grid 5×5:** 4-in-a-row to win (prevents frequent draws and keeps play fast-paced).
4. **Ultimate Tic-Tac-Toe:** A 3×3 grid of 3×3 local boards. Winning a local board claims that cell on the macro grid; winning 3 local boards in a row wins the match. Each move dictates the local board the opponent must play on next.

All variants support both **Pass & Play (2 Players)** and **Vs Computer (AI)** with scaled difficulties, integrated into the persistent progression and stats system across Android, iOS, and Desktop.

---

## Architecture & Core Domain (`core` module)

### 1. Variants and Moves
```kotlin
enum class GameVariant(val displayName: String) {
    CLASSIC_3X3("Classic (3×3)"),
    GRID_4X4("Grid (4×4)"),
    GRID_5X5("Grid (5×5)"),
    ULTIMATE("Ultimate");
}

sealed interface GameMove {
    data class Grid(val index: Int) : GameMove
    data class Ultimate(val macroIndex: Int, val microIndex: Int) : GameMove
}
```

### 2. Parameterized `GridBoard`
* Generalized immutable board supporting arbitrary dimensions `size` (3, 4, 5) and target win line length `winLength` (3 or 4).
* Precalculates all winning line coordinate sets upon initialization.
* `place(index: Int, mark: Mark): GridBoard` validates cell vacancy and returns an updated board.
* `outcome(): Outcome` checks precalculated lines for 3-in-a-row or 4-in-a-row completions.

### 3. Composite `UltimateBoard`
* **State:**
  * `subBoards: List<GridBoard>` (9 mini-boards of size 3×3).
  * `macroBoard: GridBoard` (3×3 grid representing the state of won/drawn sub-boards).
  * `activeMacroIndex: Int?` (the sub-board index where the next move must be played, or `null` if any non-terminal sub-board can be chosen).
* **Game Rules:**
  * If `activeMacroIndex` is specified, the move must target `subBoards[activeMacroIndex]`.
  * When a move is placed at `microIndex` within `macroIndex`, the sub-board outcome is evaluated. If won, `macroBoard` marks that cell.
  * If `subBoards[microIndex]` is still `Outcome.InProgress`, `nextActiveMacroIndex = microIndex`.
  * If `subBoards[microIndex]` is finished (`Won` or `Draw`), `nextActiveMacroIndex = null` (free choice of any unfinished sub-board).
  * If a sub-board ends in a draw, that macro cell is dead/neutral. Overall match is won by 3 claimed sub-boards in a row on the macro board, or ends in a draw when all playable boards are exhausted without a winner.

---

## AI Player Design

Exhaustive minimax is retained for 3×3. For 4×4, 5×5, and Ultimate, AI strategies use depth-limited Alpha-Beta pruning with heuristic evaluations running asynchronously on `Dispatchers.Default`:

### 1. Grid AI (4×4 and 5×5)
* **Search Depth:** Depth 4 for 4×4; Depth 3 for 5×5.
* **Heuristic Scoring Function:**
  * Line of 4 (Terminal Win): $+10,000$.
  * Line of 3 with open ends (Immediate Win Threat): $+150$.
  * Line of 2 with open ends: $+20$.
  * Center and inner square control: $+10$.
  * Defensive Priority: Evaluates and neutralizes opponent 3-in-a-row threats before developing attacks.
* **Difficulty Modes:**
  * **Easy:** Depth 1 search; detects immediate 1-move wins/blocks, plays randomly otherwise.
  * **Medium:** Depth 2 search with a 25% chance of selecting a suboptimal legal move.
  * **Hard / Unbeatable:** Full depth Alpha-Beta evaluation with optimal tactical blocking.

### 2. Ultimate AI
* **Hierarchical Evaluator (Depth 2–3 lookahead):**
  * Macro Alignment: $+500$ for two aligned won mini-boards.
  * Mini-Board Capture: $+100$ (center mini-board $+150$).
  * Contested Micro-Tactics: $+10$ for local threats.
  * Opponent Steering Penalty: $-60$ penalty for directing opponent to a finished board (granting free choice) or into a board where opponent has an immediate win.

---

## Progression, Stats & Data Persistence

### 1. Saved Progress v2
* Increment progress schema `version = 2`.
* Migrate stats from `Map<Difficulty, DifficultyStats>` to `variantStats: Map<String, DifficultyStats>` with key format `"${variant.name}:${difficulty.name}"`.
* Existing v1 records seamlessly default to `CLASSIC_3X3:<DIFFICULTY>` with no data loss.

### 2. Scaled XP Matrix
* **Classic 3×3:** 10 XP (Easy), 20 XP (Medium), 40 XP (Unbeatable).
* **Grid 4×4:** 20 XP (Easy), 35 XP (Medium), 60 XP (Hard).
* **Grid 5×5:** 30 XP (Easy), 50 XP (Medium), 80 XP (Hard).
* **Ultimate:** 50 XP (Easy), 80 XP (Medium), 120 XP (Hard).
* Minimum 2–10 XP participation award for draws and losses.

### 3. Daily & Weekend Challenges
* **Mon–Fri:** Classic 3×3 deterministic forced-win daily challenge.
* **Sat–Sun:** Special weekend challenge featuring tactical scenarios on 4×4 or Ultimate boards awarding $+100$ bonus XP.

---

## UI/UX Architecture (`app/shared` module)

### 1. Variant & Mode Flow
* Home screen preserves the Cyber-Neon theme.
* Tapping "Play vs Computer" or "2 Players" triggers a Variant Picker sheet:
  * Cards for `Classic (3×3)`, `Grid (4×4)`, `Grid (5×5)`, and `Ultimate`.
  * Selecting a variant in vs Computer mode transitions directly to the difficulty selector.

### 2. Dynamic Board Views
* **`AdaptiveGridBoard`:** Renders 3×3, 4×4, and 5×5 grids dynamically with scaling line strokes, glowing cell highlights, and winning line laser animations.
* **`UltimateBoard`:**
  * Renders a 3×3 composite grid containing 9 local mini-boards.
  * Highlights the current active sub-board with an animated pulsing neon border.
  * Displays bold, semi-transparent winning marks over claimed sub-boards.
  * Guarantees accessible 48dp+ tap targets across mobile, tablet, and desktop viewports.

---

## Verification & Testing Strategy
* **Unit Tests (`core`):**
  * Parameterized tests for `GridBoard(4, 4)` and `GridBoard(5, 4)` verifying horizontal, vertical, diagonal win detection and draw states.
  * Comprehensive state tests for `UltimateBoard` (valid moves, active sub-board redirection, sub-board wins, free-choice fallbacks, overall macro win).
  * Benchmark tests for Grid AI and Ultimate AI verifying execution finishes well under 200ms per move.
  * Migration test verifying v1 `PlayerProgress` deserializes cleanly into v2 without data corruption.
* **UI Tests (`app/shared`):**
  * Compose tests validating board rendering and user click interactions across 3×3, 4×4, 5×5, and Ultimate.
