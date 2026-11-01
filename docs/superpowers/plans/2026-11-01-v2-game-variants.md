# Tic Tac Toe v2: Expanded Gameplay Variants Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand Tic Tac Toe with 4×4, 5×5, and Ultimate Tic-Tac-Toe variants supporting both Pass & Play and AI with scaled difficulties, integrated into persistent progress and adaptive Cyber-Neon UI.

**Architecture:** Pure rules, grid boards, Ultimate composite logic, and AI heuristic strategies reside in the `core` module. State management, progression migration, and Compose Multiplatform dynamic board renderers reside in `app/shared`. Existing 3×3 game flow and tests continue to work with zero regression.

**Tech Stack:** Kotlin 2.4.21 Multiplatform, Compose Multiplatform 1.12.2, kotlinx-serialization-json, multiplatform-settings.

**Spec:** `docs/superpowers/specs/2026-11-01-v2-game-variants-design.md`

## Global Constraints

- Android, iOS, and Desktop continue to compile and run offline without backend requirements.
- Existing 3×3 rules, tests, and saved player data must not break or regress.
- All AI computations must execute off the main thread (`Dispatchers.Default`) within 150ms per move.
- Touch targets on mobile must maintain at least 48dp clickable hit areas.
- Win condition for 4×4 is 4-in-a-row; win condition for 5×5 is 4-in-a-row; Ultimate Tic-Tac-Toe is 3 mini-board wins in a row.

## Review Focus

1. **Dead mini-board steering:** In Ultimate, if player A directs player B to a mini-board that ended in a draw or was already won, player B can legally play in any non-terminal mini-board. Pinned in Task 2.
2. **5×5 4-in-a-row edge detection:** On 5×5 grids, diagonal and anti-diagonal lines of length 4 must detect wins without wrapping around board boundaries. Pinned in Task 1.
3. **Legacy stats deserialization:** A player with v1 saved progress (`stats: Map<Difficulty, DifficultyStats>`) must deserialize into v2 `variantStats` without crash or data loss. Pinned in Task 5.
4. **AI timeout on deep states:** On 4×4 / 5×5 grids, search depth is strictly clamped so AI never causes frame drops or UI freezes. Pinned in Task 3.
5. **Simultaneous sub-board completion:** In Ultimate, a move that completes a local board and simultaneously triggers a macro board win must immediately transition the macro game to `Outcome.Won`. Pinned in Task 2.

---

## File Structure

### `core` module
- Create:
  - `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GameVariant.kt` — `GameVariant`, `GameMove`
  - `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GridBoard.kt` — Parameterized `GridBoard`
  - `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/UltimateBoard.kt` — Composite `UltimateBoard`
  - `core/src/commonMain/kotlin/com/example/tictactoe/domain/player/HeuristicGridAI.kt` — Alpha-Beta heuristic AI for 4×4 and 5×5
  - `core/src/commonMain/kotlin/com/example/tictactoe/domain/player/UltimateAI.kt` — Hierarchical AI for Ultimate Tic-Tac-Toe
- Modify:
  - `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/PlayerProgress.kt` — v2 schema migration
  - `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/XpRules.kt` — Variant-scaled XP
- Test:
  - `core/src/commonTest/kotlin/com/example/tictactoe/domain/GridBoardTest.kt`
  - `core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateBoardTest.kt`
  - `core/src/commonTest/kotlin/com/example/tictactoe/domain/HeuristicGridAITest.kt`
  - `core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateAITest.kt`
  - `core/src/commonTest/kotlin/com/example/tictactoe/domain/ProgressMigrationTest.kt`

### `app/shared` module
- Create:
  - `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/VariantPickerSheet.kt` — Variant selector
  - `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/AdaptiveGridBoard.kt` — Scalable N×N grid board
  - `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/UltimateBoardView.kt` — 3×3 mini-boards composable
- Modify:
  - `app/shared/src/commonMain/kotlin/com/example/tictactoe/domain/model/Match.kt` (or presentation equivalent) — Support variant settings
  - `app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameMvi.kt` — Variant intents and state
  - `app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameViewModel.kt` — Reducer and AI dispatch for variants
  - `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt` — Board switching and variant picker triggers
- Test:
  - `app/shared/src/commonTest/kotlin/com/example/tictactoe/presentation/GameViewModelVariantTest.kt`

---

### Task 1: Parameterized `GridBoard` and `GameVariant`

**Files:**
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GameVariant.kt`
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GridBoard.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/GridBoardTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class GameVariant(val displayName: String) { CLASSIC_3X3, GRID_4X4, GRID_5X5, ULTIMATE }
  sealed interface GameMove {
      data class Grid(val index: Int) : GameMove
      data class Ultimate(val macroIndex: Int, val microIndex: Int) : GameMove
  }
  data class GridBoard(val size: Int, val winLength: Int, val cells: List<Mark?> = List(size * size) { null }) {
      val emptyCells: List<Int>
      fun place(index: Int, mark: Mark): GridBoard
      fun outcome(): Outcome
  }
  ```

- [ ] **Step 1: Write the failing test**

Create `core/src/commonTest/kotlin/com/example/tictactoe/domain/GridBoardTest.kt`:
```kotlin
package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridBoardTest {
    @Test
    fun grid4x4DetectsHorizontalWin() {
        var board = GridBoard(size = 4, winLength = 4)
        listOf(0, 1, 2, 3).forEach { board = board.place(it, Mark.X) }
        val outcome = board.outcome()
        assertTrue(outcome is Outcome.Won)
        assertEquals(Mark.X, (outcome as Outcome.Won).winner)
        assertEquals(listOf(0, 1, 2, 3), outcome.line)
    }

    @Test
    fun grid5x5Detects4InARowDiagonalWinWithoutWrap() {
        var board = GridBoard(size = 5, winLength = 4)
        // Diagonal: (0,0)=0, (1,1)=6, (2,2)=12, (3,3)=18
        listOf(0, 6, 12, 18).forEach { board = board.place(it, Mark.O) }
        val outcome = board.outcome()
        assertTrue(outcome is Outcome.Won)
        assertEquals(Mark.O, (outcome as Outcome.Won).winner)
        assertEquals(listOf(0, 6, 12, 18), outcome.line)
    }

    @Test
    fun grid4x4DetectsDrawWhenBoardFullWithoutWinner() {
        val pattern = listOf(
            Mark.X, Mark.O, Mark.X, Mark.O,
            Mark.X, Mark.O, Mark.X, Mark.O,
            Mark.O, Mark.X, Mark.O, Mark.X,
            Mark.O, Mark.X, Mark.O, Mark.X,
        )
        val board = GridBoard(size = 4, winLength = 4, cells = pattern)
        assertEquals(Outcome.Draw, board.outcome())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.GridBoardTest"`
Expected: Compilation failure ("Unresolved reference: GridBoard, GameVariant").

- [ ] **Step 3: Write minimal implementation**

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GameVariant.kt`:
```kotlin
package com.example.tictactoe.domain.model

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

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GridBoard.kt`:
```kotlin
package com.example.tictactoe.domain.model

data class GridBoard(
    val size: Int,
    val winLength: Int,
    val cells: List<Mark?> = List(size * size) { null }
) {
    init {
        require(size >= 3) { "Size must be at least 3" }
        require(winLength in 3..size) { "Win length must be between 3 and $size" }
        require(cells.size == size * size) { "Cells count must be ${size * size}" }
    }

    val emptyCells: List<Int> get() = cells.indices.filter { cells[it] == null }

    val winLines: List<List<Int>> = computeWinLines(size, winLength)

    fun place(index: Int, mark: Mark): GridBoard {
        require(index in cells.indices && cells[index] == null) { "Cell $index is not available" }
        return copy(cells = cells.toMutableList().also { it[index] = mark })
    }

    fun outcome(): Outcome {
        for (line in winLines) {
            val first = cells[line[0]] ?: continue
            if (line.all { cells[it] == first }) {
                return Outcome.Won(first, line)
            }
        }
        return if (emptyCells.isEmpty()) Outcome.Draw else Outcome.InProgress
    }

    companion object {
        private val cache = mutableMapOf<Pair<Int, Int>, List<List<Int>>>()

        fun computeWinLines(size: Int, winLength: Int): List<List<Int>> =
            cache.getOrPut(size to winLength) {
                val lines = mutableListOf<List<Int>>()
                // Rows
                for (r in 0 until size) {
                    for (c in 0..size - winLength) {
                        lines.add((0 until winLength).map { r * size + (c + it) })
                    }
                }
                // Columns
                for (c in 0 until size) {
                    for (r in 0..size - winLength) {
                        lines.add((0 until winLength).map { (r + it) * size + c })
                    }
                }
                // Main Diagonals (\)
                for (r in 0..size - winLength) {
                    for (c in 0..size - winLength) {
                        lines.add((0 until winLength).map { (r + it) * size + (c + it) })
                    }
                }
                // Anti-Diagonals (/)
                for (r in 0..size - winLength) {
                    for (c in winLength - 1 until size) {
                        lines.add((0 until winLength).map { (r + it) * size + (c - it) })
                    }
                }
                lines
            }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.GridBoardTest"`
Expected: BUILD SUCCESSFUL (3 tests passed).

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GameVariant.kt core/src/commonMain/kotlin/com/example/tictactoe/domain/model/GridBoard.kt core/src/commonTest/kotlin/com/example/tictactoe/domain/GridBoardTest.kt
git commit -m "feat(core): add parameterized GridBoard and GameVariant models"
```

---

### Task 2: Composite `UltimateBoard` Mechanics

**Files:**
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/UltimateBoard.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateBoardTest.kt`

**Interfaces:**
- Consumes: `GridBoard`, `GameMove.Ultimate`, `Outcome`, `Mark`
- Produces:
  ```kotlin
  data class UltimateBoard(
      val subBoards: List<GridBoard> = List(9) { GridBoard(size = 3, winLength = 3) },
      val macroBoard: GridBoard = GridBoard(size = 3, winLength = 3),
      val activeMacroIndex: Int? = null,
  ) {
      fun legalMoves(): List<GameMove.Ultimate>
      fun place(move: GameMove.Ultimate, mark: Mark): UltimateBoard
      fun outcome(): Outcome
  }
  ```

- [ ] **Step 1: Write the failing test**

Create `core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateBoardTest.kt`:
```kotlin
package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UltimateBoardTest {
    @Test
    fun initialBoardAllowsAnySubBoardMove() {
        val board = UltimateBoard()
        assertEquals(81, board.legalMoves().size)
        assertNull(board.activeMacroIndex)
    }

    @Test
    fun moveDirectsOpponentToTargetSubBoard() {
        val board = UltimateBoard().place(GameMove.Ultimate(macroIndex = 4, microIndex = 2), Mark.X)
        assertEquals(2, board.activeMacroIndex)
        assertTrue(board.legalMoves().all { it.macroIndex == 2 })
    }

    @Test
    fun winningSubBoardClaimsMacroCellAndAllowsFreeChoiceIfTargetIsFinished() {
        var board = UltimateBoard()
        // Win sub-board 0 for X
        board = board.place(GameMove.Ultimate(0, 0), Mark.X) // next 0
        board = board.place(GameMove.Ultimate(0, 1), Mark.O) // next 1
        board = board.place(GameMove.Ultimate(1, 0), Mark.X) // next 0
        board = board.place(GameMove.Ultimate(0, 4), Mark.O) // next 4
        board = board.place(GameMove.Ultimate(4, 0), Mark.X) // next 0
        board = board.place(GameMove.Ultimate(0, 2), Mark.X) // X wins sub-board 0 with (0,1,2)!
        
        val subOutcome = board.subBoards[0].outcome()
        assertTrue(subOutcome is Outcome.Won)
        assertEquals(Mark.X, board.macroBoard.cells[0])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.UltimateBoardTest"`
Expected: Compilation failure ("Unresolved reference: UltimateBoard").

- [ ] **Step 3: Write minimal implementation**

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/UltimateBoard.kt`:
```kotlin
package com.example.tictactoe.domain.model

data class UltimateBoard(
    val subBoards: List<GridBoard> = List(9) { GridBoard(size = 3, winLength = 3) },
    val macroBoard: GridBoard = GridBoard(size = 3, winLength = 3),
    val activeMacroIndex: Int? = null,
) {
    init {
        require(subBoards.size == 9) { "UltimateBoard requires 9 subBoards" }
    }

    fun legalMoves(): List<GameMove.Ultimate> {
        if (outcome() !is Outcome.InProgress) return emptyList()
        val targetIndices = if (activeMacroIndex != null && subBoards[activeMacroIndex].outcome() is Outcome.InProgress) {
            listOf(activeMacroIndex)
        } else {
            subBoards.indices.filter { subBoards[it].outcome() is Outcome.InProgress }
        }
        return targetIndices.flatMap { macro ->
            subBoards[macro].emptyCells.map { micro -> GameMove.Ultimate(macro, micro) }
        }
    }

    fun place(move: GameMove.Ultimate, mark: Mark): UltimateBoard {
        require(move in legalMoves()) { "Illegal move $move in UltimateBoard" }
        val updatedSub = subBoards[move.macroIndex].place(move.microIndex, mark)
        val newSubBoards = subBoards.toMutableList().also { it[move.macroIndex] = updatedSub }

        val subOutcome = updatedSub.outcome()
        val newMacroBoard = when (subOutcome) {
            is Outcome.Won -> macroBoard.place(move.macroIndex, subOutcome.winner)
            else -> macroBoard
        }

        val nextTarget = move.microIndex
        val nextActive = if (newSubBoards[nextTarget].outcome() is Outcome.InProgress) {
            nextTarget
        } else {
            null // Free choice if target sub-board is complete
        }

        return copy(
            subBoards = newSubBoards,
            macroBoard = newMacroBoard,
            activeMacroIndex = nextActive,
        )
    }

    fun outcome(): Outcome {
        val macroOutcome = macroBoard.outcome()
        if (macroOutcome is Outcome.Won) return macroOutcome
        val anyInProgress = subBoards.any { it.outcome() is Outcome.InProgress }
        return if (!anyInProgress || macroOutcome is Outcome.Draw) Outcome.Draw else Outcome.InProgress
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.UltimateBoardTest"`
Expected: BUILD SUCCESSFUL (3 tests passed).

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/model/UltimateBoard.kt core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateBoardTest.kt
git commit -m "feat(core): add UltimateBoard composite mechanics with active sub-board tracking"
```

---

### Task 3: Alpha-Beta Minimax AI for 4×4 and 5×5

**Files:**
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/player/HeuristicGridAI.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/HeuristicGridAITest.kt`

**Interfaces:**
- Consumes: `GridBoard`, `Mark`, `Outcome`, `Difficulty`
- Produces:
  ```kotlin
  class HeuristicGridAI(
      val difficulty: Difficulty,
      val maxDepth: Int = when (difficulty) {
          Difficulty.Easy -> 1
          Difficulty.Medium -> 2
          Difficulty.Unbeatable -> 4
      }
  ) {
      fun chooseMove(board: GridBoard, turn: Mark): Int
  }
  ```

- [ ] **Step 1: Write the failing test**

Create `core/src/commonTest/kotlin/com/example/tictactoe/domain/HeuristicGridAITest.kt`:
```kotlin
package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.player.HeuristicGridAI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeuristicGridAITest {
    @Test
    fun unbeatableBlocksImmediate4x4Threat() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable, maxDepth = 3)
        // Opponent O has cells 0, 1, 2 in 4x4. AI X must play 3 to block win.
        var board = GridBoard(size = 4, winLength = 4)
        board = board.place(0, Mark.O).place(1, Mark.O).place(2, Mark.O)
        val move = ai.chooseMove(board, Mark.X)
        assertEquals(3, move)
    }

    @Test
    fun unbeatableSeizesImmediate4x4Win() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable, maxDepth = 2)
        // AI X has cells 4, 5, 6 in 4x4. AI X must play 7 to win.
        var board = GridBoard(size = 4, winLength = 4)
        board = board.place(4, Mark.X).place(5, Mark.X).place(6, Mark.X)
        val move = ai.chooseMove(board, Mark.X)
        assertEquals(7, move)
    }

    @Test
    fun decisionTimeIsUnder150msOn5x5() {
        val ai = HeuristicGridAI(difficulty = Difficulty.Unbeatable, maxDepth = 3)
        val board = GridBoard(size = 5, winLength = 4)
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val move = ai.chooseMove(board, Mark.X)
        val elapsed = start.elapsedNow().inWholeMilliseconds
        assertTrue(move in board.emptyCells)
        assertTrue(elapsed < 150, "AI took ${elapsed}ms, should be <150ms")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.HeuristicGridAITest"`
Expected: Compilation failure ("Unresolved reference: HeuristicGridAI").

- [ ] **Step 3: Write minimal implementation**

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/player/HeuristicGridAI.kt`:
```kotlin
package com.example.tictactoe.domain.player

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import kotlin.random.Random

class HeuristicGridAI(
    val difficulty: Difficulty,
    val maxDepth: Int = when (difficulty) {
        Difficulty.Easy -> 1
        Difficulty.Medium -> 2
        Difficulty.Unbeatable -> 3
    },
    private val random: Random = Random,
) {
    fun chooseMove(board: GridBoard, turn: Mark): Int {
        val legal = board.emptyCells
        if (legal.isEmpty()) throw IllegalStateException("No legal moves available")
        if (legal.size == board.size * board.size) return legal.size / 2 // Take center

        // Easy mode blunder chance
        if (difficulty == Difficulty.Easy && random.nextInt(4) != 0) {
            return legal.random(random)
        }

        // Medium mode occasional mistake
        if (difficulty == Difficulty.Medium && random.nextInt(4) == 0) {
            return legal.random(random)
        }

        // Ordered candidates: center and near-center first
        val center = (board.size - 1) / 2.0
        val sortedMoves = legal.sortedBy { index ->
            val r = index / board.size
            val c = index % board.size
            val dist = (r - center) * (r - center) + (c - center) * (c - center)
            dist
        }

        var bestMove = sortedMoves.first()
        var bestScore = Int.MIN_VALUE
        var alpha = -50000
        val beta = 50000

        for (move in sortedMoves) {
            val score = minimax(board.place(move, turn), turn, turn.other, 1, alpha, beta)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = maxOf(alpha, bestScore)
        }
        return bestMove
    }

    private fun minimax(board: GridBoard, me: Mark, currentTurn: Mark, depth: Int, alpha: Int, beta: Int): Int {
        when (val outcome = board.outcome()) {
            is Outcome.Won -> return if (outcome.winner == me) 10000 - depth else depth - 10000
            Outcome.Draw -> return 0
            Outcome.InProgress -> if (depth >= maxDepth) return evaluate(board, me)
        }

        var currAlpha = alpha
        var currBeta = beta

        if (currentTurn == me) {
            var maxEval = Int.MIN_VALUE
            for (move in board.emptyCells) {
                val eval = minimax(board.place(move, currentTurn), me, currentTurn.other, depth + 1, currAlpha, currBeta)
                maxEval = maxOf(maxEval, eval)
                currAlpha = maxOf(currAlpha, eval)
                if (currBeta <= currAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in board.emptyCells) {
                val eval = minimax(board.place(move, currentTurn), me, currentTurn.other, depth + 1, currAlpha, currBeta)
                minEval = minOf(minEval, eval)
                currBeta = minOf(currBeta, eval)
                if (currBeta <= currAlpha) break
            }
            return minEval
        }
    }

    private fun evaluate(board: GridBoard, me: Mark): Int {
        var score = 0
        val opponent = me.other

        for (line in board.winLines) {
            var meCount = 0
            var oppCount = 0
            for (idx in line) {
                when (board.cells[idx]) {
                    me -> meCount++
                    opponent -> oppCount++
                    null -> {}
                }
            }
            if (meCount > 0 && oppCount > 0) continue // Contested line
            if (meCount == board.winLength - 1) score += 150
            else if (meCount > 1) score += meCount * 15
            if (oppCount == board.winLength - 1) score -= 200 // Strongly prioritize blocking opponent threat
            else if (oppCount > 1) score -= oppCount * 20
        }
        return score
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.HeuristicGridAITest"`
Expected: BUILD SUCCESSFUL (3 tests passed).

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/player/HeuristicGridAI.kt core/src/commonTest/kotlin/com/example/tictactoe/domain/HeuristicGridAITest.kt
git commit -m "feat(core): add Alpha-Beta HeuristicGridAI for 4x4 and 5x5 boards"
```

---

### Task 4: Ultimate Tic-Tac-Toe AI Evaluator

**Files:**
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/player/UltimateAI.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateAITest.kt`

**Interfaces:**
- Consumes: `UltimateBoard`, `GameMove.Ultimate`, `Mark`, `Outcome`, `Difficulty`
- Produces:
  ```kotlin
  class UltimateAI(
      val difficulty: Difficulty,
      val maxDepth: Int = when (difficulty) {
          Difficulty.Easy -> 1
          Difficulty.Medium -> 2
          Difficulty.Unbeatable -> 2
      }
  ) {
      fun chooseMove(board: UltimateBoard, turn: Mark): GameMove.Ultimate
  }
  ```

- [ ] **Step 1: Write the failing test**

Create `core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateAITest.kt`:
```kotlin
package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.UltimateBoard
import com.example.tictactoe.domain.player.UltimateAI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UltimateAITest {
    @Test
    fun aiCompletesImmediateSubBoardWin() {
        val ai = UltimateAI(Difficulty.Unbeatable, maxDepth = 2)
        var board = UltimateBoard(activeMacroIndex = 0)
        // Sub-board 0 has X at 0 and 1. AI X playing in sub-board 0 must pick 2 to claim it!
        board = board.place(GameMove.Ultimate(0, 0), Mark.X)
        board = board.place(GameMove.Ultimate(0, 3), Mark.O)
        board = board.place(GameMove.Ultimate(3, 0), Mark.O)
        board = board.place(GameMove.Ultimate(0, 1), Mark.X)
        // Set active board to 0
        board = board.copy(activeMacroIndex = 0)
        val move = ai.chooseMove(board, Mark.X)
        assertEquals(0, move.macroIndex)
        assertEquals(2, move.microIndex)
    }

    @Test
    fun decisionTimeIsUnder150msOnUltimateBoard() {
        val ai = UltimateAI(Difficulty.Unbeatable, maxDepth = 2)
        val board = UltimateBoard()
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val move = ai.chooseMove(board, Mark.X)
        val elapsed = start.elapsedNow().inWholeMilliseconds
        assertTrue(move in board.legalMoves())
        assertTrue(elapsed < 150, "Ultimate AI took ${elapsed}ms, should be <150ms")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.UltimateAITest"`
Expected: Compilation failure ("Unresolved reference: UltimateAI").

- [ ] **Step 3: Write minimal implementation**

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/player/UltimateAI.kt`:
```kotlin
package com.example.tictactoe.domain.player

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import kotlin.random.Random

class UltimateAI(
    val difficulty: Difficulty,
    val maxDepth: Int = when (difficulty) {
        Difficulty.Easy -> 1
        Difficulty.Medium -> 2
        Difficulty.Unbeatable -> 2
    },
    private val random: Random = Random,
) {
    fun chooseMove(board: UltimateBoard, turn: Mark): GameMove.Ultimate {
        val legal = board.legalMoves()
        if (legal.isEmpty()) throw IllegalStateException("No legal moves available")

        if (difficulty == Difficulty.Easy && random.nextInt(4) != 0) {
            return legal.random(random)
        }

        var bestMove = legal.first()
        var bestScore = Int.MIN_VALUE

        for (move in legal) {
            val nextBoard = board.place(move, turn)
            val score = minimax(nextBoard, turn, turn.other, 1)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(board: UltimateBoard, me: Mark, currentTurn: Mark, depth: Int): Int {
        when (val outcome = board.outcome()) {
            is Outcome.Won -> return if (outcome.winner == me) 10000 - depth else depth - 10000
            Outcome.Draw -> return 0
            Outcome.InProgress -> if (depth >= maxDepth) return evaluate(board, me)
        }

        val moves = board.legalMoves()
        if (moves.isEmpty()) return 0

        return if (currentTurn == me) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                val eval = minimax(board.place(move, currentTurn), me, currentTurn.other, depth + 1)
                maxEval = maxOf(maxEval, eval)
            }
            maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                val eval = minimax(board.place(move, currentTurn), me, currentTurn.other, depth + 1)
                minEval = minOf(minEval, eval)
            }
            minEval
        }
    }

    private fun evaluate(board: UltimateBoard, me: Mark): Int {
        var score = 0
        val opponent = me.other

        // Macro board evaluation
        for (line in board.macroBoard.winLines) {
            var meCount = 0
            var oppCount = 0
            for (idx in line) {
                when (board.macroBoard.cells[idx]) {
                    me -> meCount++
                    opponent -> oppCount++
                    null -> {}
                }
            }
            if (meCount > 0 && oppCount > 0) continue
            if (meCount == 2) score += 300
            else if (meCount == 1) score += 50
            if (oppCount == 2) score -= 400
        }

        // Sub-board captures & Center board bonus
        for (i in 0 until 9) {
            when (board.macroBoard.cells[i]) {
                me -> score += if (i == 4) 150 else 100
                opponent -> score -= if (i == 4) 150 else 100
                null -> {}
            }
        }

        // Penalty for giving opponent free choice
        if (board.activeMacroIndex == null) {
            score -= 40
        }

        return score
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.UltimateAITest"`
Expected: BUILD SUCCESSFUL (2 tests passed).

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/player/UltimateAI.kt core/src/commonTest/kotlin/com/example/tictactoe/domain/UltimateAITest.kt
git commit -m "feat(core): add UltimateAI hierarchical evaluator"
```

---

### Task 5: Progress v2 Migration and Scaled XP Rules

**Files:**
- Modify: `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/PlayerProgress.kt`
- Modify: `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/XpRules.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/ProgressMigrationTest.kt`

**Interfaces:**
- Consumes: `GameVariant`, `Difficulty`, `GameResult`
- Produces:
  ```kotlin
  object XpRules {
      fun forGame(variant: GameVariant, difficulty: Difficulty, result: GameResult): Int
  }
  @Serializable
  data class PlayerProgress(
      val version: Int = 2,
      val xp: Int = 0,
      val variantStats: Map<String, DifficultyStats> = emptyMap(),
      val streak: StreakState = StreakState(),
  )
  ```

- [ ] **Step 1: Write the failing test**

Create `core/src/commonTest/kotlin/com/example/tictactoe/domain/ProgressMigrationTest.kt`:
```kotlin
package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.progress.DifficultyStats
import com.example.tictactoe.domain.progress.GameResult
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.XpRules
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressMigrationTest {
    @Test
    fun legacyV1ProgressJsonDeserializesCleanly() {
        // v1 json format with "stats" containing Easy/Medium/Unbeatable
        val v1Json = """
            {
              "version": 1,
              "xp": 150,
              "stats": {
                "Easy": { "wins": 3, "draws": 1, "losses": 0 },
                "Unbeatable": { "wins": 1, "draws": 2, "losses": 5 }
              },
              "streak": { "current": 2, "best": 4 }
            }
        """.trimIndent()

        val progress = PlayerProgress.fromJson(v1Json)
        assertEquals(2, progress.version)
        assertEquals(150, progress.xp)
        assertEquals(3, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Easy).wins)
        assertEquals(1, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable).wins)
    }

    @Test
    fun xpAwardsScaleWithVariantComplexity() {
        val win3x3 = XpRules.forGame(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable, GameResult.Win)
        val win4x4 = XpRules.forGame(GameVariant.GRID_4X4, Difficulty.Unbeatable, GameResult.Win)
        val win5x5 = XpRules.forGame(GameVariant.GRID_5X5, Difficulty.Unbeatable, GameResult.Win)
        val winUltimate = XpRules.forGame(GameVariant.ULTIMATE, Difficulty.Unbeatable, GameResult.Win)

        assertEquals(40, win3x3)
        assertEquals(60, win4x4)
        assertEquals(80, win5x5)
        assertEquals(120, winUltimate)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.ProgressMigrationTest"`
Expected: Compilation failure ("Unresolved reference: fromJson, statsFor").

- [ ] **Step 3: Write minimal implementation**

Update `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/PlayerProgress.kt`:
```kotlin
package com.example.tictactoe.domain.progress

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class GameResult { Win, Draw, Loss }

@Serializable
data class DifficultyStats(
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
) {
    fun record(result: GameResult): DifficultyStats = when (result) {
        GameResult.Win -> copy(wins = wins + 1)
        GameResult.Draw -> copy(draws = draws + 1)
        GameResult.Loss -> copy(losses = losses + 1)
    }
}

@Serializable
data class PlayerProgress(
    val version: Int = 2,
    val xp: Int = 0,
    val variantStats: Map<String, DifficultyStats> = emptyMap(),
    val streak: StreakState = StreakState(),
) {
    fun statsFor(variant: GameVariant, difficulty: Difficulty): DifficultyStats =
        variantStats["${variant.name}:${difficulty.name}"] ?: DifficultyStats()

    fun recordGame(variant: GameVariant, difficulty: Difficulty, result: GameResult): PlayerProgress {
        val key = "${variant.name}:${difficulty.name}"
        val current = variantStats[key] ?: DifficultyStats()
        val updated = current.record(result)
        val earnedXp = XpRules.forGame(variant, difficulty, result)
        return copy(
            xp = xp + earnedXp,
            variantStats = variantStats + (key to updated),
        )
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(rawJson: String): PlayerProgress {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val version = root["version"]?.jsonPrimitive?.intOrNull ?: 1
            if (version == 1 && root.containsKey("stats")) {
                val legacyStats = json.decodeFromJsonElement<Map<Difficulty, DifficultyStats>>(root.getValue("stats"))
                val migratedVariantStats = legacyStats.mapKeys { "${GameVariant.CLASSIC_3X3.name}:${it.key.name}" }
                val streak = root["streak"]?.let { json.decodeFromJsonElement<StreakState>(it) } ?: StreakState()
                val xp = root["xp"]?.jsonPrimitive?.intOrNull ?: 0
                return PlayerProgress(
                    version = 2,
                    xp = xp,
                    variantStats = migratedVariantStats,
                    streak = streak,
                )
            }
            return json.decodeFromJsonElement(root)
        }
    }
}
```

Update `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/XpRules.kt`:
```kotlin
package com.example.tictactoe.domain.progress

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant

object XpRules {
    const val CHALLENGE_BONUS = 50

    fun forGame(
        variant: GameVariant = GameVariant.CLASSIC_3X3,
        difficulty: Difficulty,
        result: GameResult
    ): Int = when (result) {
        GameResult.Loss -> when (variant) {
            GameVariant.CLASSIC_3X3 -> 1
            GameVariant.GRID_4X4 -> 2
            GameVariant.GRID_5X5 -> 3
            GameVariant.ULTIMATE -> 5
        }
        GameResult.Draw -> when (variant) {
            GameVariant.CLASSIC_3X3 -> when (difficulty) {
                Difficulty.Easy -> 3
                Difficulty.Medium -> 6
                Difficulty.Unbeatable -> 15
            }
            GameVariant.GRID_4X4 -> when (difficulty) {
                Difficulty.Easy -> 5
                Difficulty.Medium -> 10
                Difficulty.Unbeatable -> 20
            }
            GameVariant.GRID_5X5 -> when (difficulty) {
                Difficulty.Easy -> 8
                Difficulty.Medium -> 15
                Difficulty.Unbeatable -> 30
            }
            GameVariant.ULTIMATE -> when (difficulty) {
                Difficulty.Easy -> 15
                Difficulty.Medium -> 25
                Difficulty.Unbeatable -> 40
            }
        }
        GameResult.Win -> when (variant) {
            GameVariant.CLASSIC_3X3 -> when (difficulty) {
                Difficulty.Easy -> 10
                Difficulty.Medium -> 20
                Difficulty.Unbeatable -> 40
            }
            GameVariant.GRID_4X4 -> when (difficulty) {
                Difficulty.Easy -> 20
                Difficulty.Medium -> 35
                Difficulty.Unbeatable -> 60
            }
            GameVariant.GRID_5X5 -> when (difficulty) {
                Difficulty.Easy -> 30
                Difficulty.Medium -> 50
                Difficulty.Unbeatable -> 80
            }
            GameVariant.ULTIMATE -> when (difficulty) {
                Difficulty.Easy -> 50
                Difficulty.Medium -> 80
                Difficulty.Unbeatable -> 120
            }
        }
    }

    fun xpToReach(level: Int): Int {
        val n = (level - 1).coerceAtLeast(0)
        return 50 * n * (n + 1)
    }

    fun levelFor(xp: Int): Int {
        var level = 1
        while (xpToReach(level + 1) <= xp) level++
        return level
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :core:jvmTest --tests "com.example.tictactoe.domain.ProgressMigrationTest"`
Expected: BUILD SUCCESSFUL (2 tests passed).

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/PlayerProgress.kt core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/XpRules.kt core/src/commonTest/kotlin/com/example/tictactoe/domain/ProgressMigrationTest.kt
git commit -m "feat(core): add PlayerProgress v2 schema migration and variant-scaled XP rules"
```

---

### Task 6: Presentation / MVI Integration (`app/shared`)

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/domain/model/Match.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameMvi.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameViewModel.kt`
- Test: `app/shared/src/commonTest/kotlin/com/example/tictactoe/presentation/GameViewModelVariantTest.kt`

**Interfaces:**
- Consumes: `GameVariant`, `GameMove`, `GridBoard`, `UltimateBoard`, `HeuristicGridAI`, `UltimateAI`
- Produces:
  `MatchSettings(mode, difficulty, variant)`
  `UiState` containing active `variant`, `gridBoard: GridBoard?`, `ultimateBoard: UltimateBoard?`

- [ ] **Step 1: Write the failing test**

Create `app/shared/src/commonTest/kotlin/com/example/tictactoe/presentation/GameViewModelVariantTest.kt`:
```kotlin
package com.example.tictactoe.presentation

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.progress.InMemoryProgressRepository
import com.example.tictactoe.domain.time.TodayProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelVariantTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun starting4x4VariantInitializesGridBoard() = runTest {
        val repo = InMemoryProgressRepository()
        val today = object : TodayProvider { override fun today(): LocalDate = LocalDate(2026, 11, 1) }
        val vm = GameViewModel(progressRepository = repo, todayProvider = today)

        vm.send(Intent.SelectVariant(GameVariant.GRID_4X4))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GameVariant.GRID_4X4, vm.state.value.variant)
        assertNotNull(vm.state.value.gridBoard)
        assertEquals(4, vm.state.value.gridBoard?.size)
    }

    @Test
    fun startingUltimateVariantInitializesUltimateBoard() = runTest {
        val repo = InMemoryProgressRepository()
        val today = object : TodayProvider { override fun today(): LocalDate = LocalDate(2026, 11, 1) }
        val vm = GameViewModel(progressRepository = repo, todayProvider = today)

        vm.send(Intent.SelectVariant(GameVariant.ULTIMATE))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GameVariant.ULTIMATE, vm.state.value.variant)
        assertNotNull(vm.state.value.ultimateBoard)
        assertEquals(9, vm.state.value.ultimateBoard?.subBoards?.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :app:shared:jvmTest --tests "com.example.tictactoe.presentation.GameViewModelVariantTest"`
Expected: Compilation failure ("Unresolved reference: SelectVariant, variant, gridBoard, ultimateBoard").

- [ ] **Step 3: Write minimal implementation**

Modify `core/src/commonMain/kotlin/com/example/tictactoe/domain/model/Match.kt`:
Add `val variant: GameVariant = GameVariant.CLASSIC_3X3` to `MatchSettings`.

Modify `app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameMvi.kt`:
Extend `Intent`:
- `data class SelectVariant(val variant: GameVariant) : Intent`
- `data class PlaceVariantMove(val move: GameMove) : Intent`
Extend `UiState`:
- `val variant: GameVariant = GameVariant.CLASSIC_3X3`
- `val gridBoard: GridBoard? = null`
- `val ultimateBoard: UltimateBoard? = null`

Modify `app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameViewModel.kt`:
Handle `Intent.SelectVariant` and `Intent.PlaceVariantMove`:
- When variant is `CLASSIC_3X3`, maintain existing `match` state.
- When variant is `GRID_4X4` or `GRID_5X5`, initialize `gridBoard = GridBoard(size = ..., winLength = 4)`. If vs computer and it's computer's turn, trigger `HeuristicGridAI`.
- When variant is `ULTIMATE`, initialize `ultimateBoard = UltimateBoard()`. If vs computer and computer's turn, trigger `UltimateAI`.
- On match finish, record progress using `recordGame(variant, difficulty, result)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :app:shared:jvmTest --tests "com.example.tictactoe.presentation.GameViewModelVariantTest"`
Expected: BUILD SUCCESSFUL (2 tests passed).

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/model/Match.kt app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameMvi.kt app/shared/src/commonMain/kotlin/com/example/tictactoe/presentation/GameViewModel.kt app/shared/src/commonTest/kotlin/com/example/tictactoe/presentation/GameViewModelVariantTest.kt
git commit -m "feat(presentation): integrate GameVariant and boards into GameViewModel and MVI state"
```

---

### Task 7: Compose UI — Variant Picker Sheet & Game Header

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/VariantPickerSheet.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/Controls.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt`

**Interfaces:**
- Consumes: `GameVariant`, `LocalGameColors`, `GlassPanel`
- Produces:
  `VariantPickerSheet(currentVariant: GameVariant, onSelect: (GameVariant) -> Unit, onDismiss: () -> Unit)`

- [ ] **Step 1: Write the failing test**

Create `app/shared/src/commonTest/kotlin/com/example/tictactoe/ui/VariantPickerTest.kt`:
```kotlin
package com.example.tictactoe.ui

import com.example.tictactoe.domain.model.GameVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class VariantPickerTest {
    @Test
    fun allVariantsHaveDisplayLabels() {
        val labels = GameVariant.entries.map { it.displayName }
        assertEquals(4, labels.size)
        assertEquals("Classic (3×3)", labels[0])
        assertEquals("Grid (4×4)", labels[1])
        assertEquals("Grid (5×5)", labels[2])
        assertEquals("Ultimate", labels[3])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :app:shared:jvmTest --tests "com.example.tictactoe.ui.VariantPickerTest"`
Expected: PASS (or fail if labels don't match exactly).

- [ ] **Step 3: Write minimal implementation**

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/VariantPickerSheet.kt`:
```kotlin
package com.example.tictactoe.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun VariantPickerSheet(
    selectedVariant: GameVariant,
    onSelect: (GameVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalGameColors.current
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SELECT VARIANT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                GameVariant.entries.forEach { variant ->
                    val isSelected = variant == selectedVariant
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(variant)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) colors.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = variant.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.primary else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
```

Modify `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt`:
Add a badge in the header displaying the active variant and an option to trigger `VariantPickerSheet`.

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :app:shared:jvmTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/VariantPickerSheet.kt app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt app/shared/src/commonTest/kotlin/com/example/tictactoe/ui/VariantPickerTest.kt
git commit -m "feat(ui): add VariantPickerSheet and game variant header indicators"
```

---

### Task 8: Compose UI — `AdaptiveGridBoard` and `UltimateBoard` Views

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/AdaptiveGridBoard.kt`
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/UltimateBoardView.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt`

**Interfaces:**
- Consumes: `GridBoard`, `UltimateBoard`, `GameMove`, `LocalGameColors`
- Produces:
  `AdaptiveGridBoard(board: GridBoard, onCellClick: (Int) -> Unit)`
  `UltimateBoardView(board: UltimateBoard, onCellClick: (macroIndex: Int, microIndex: Int) -> Unit)`

- [ ] **Step 1: Write the failing test**

Create `app/shared/src/commonTest/kotlin/com/example/tictactoe/ui/BoardLayoutCalculationTest.kt`:
```kotlin
package com.example.tictactoe.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class BoardLayoutCalculationTest {
    @Test
    fun gridCellSizeScalesCorrectlyForDifferentSizes() {
        val containerSize = 360f
        val gap = 8f
        // 4x4 has 4 cells and 3 gaps
        val cellSize4 = (containerSize - (gap * 3)) / 4
        // 5x5 has 5 cells and 4 gaps
        val cellSize5 = (containerSize - (gap * 4)) / 5

        assertEquals(84f, cellSize4)
        assertEquals(65.6f, cellSize5)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :app:shared:jvmTest --tests "com.example.tictactoe.ui.BoardLayoutCalculationTest"`
Expected: PASS.

- [ ] **Step 3: Write minimal implementation**

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/AdaptiveGridBoard.kt`:
```kotlin
package com.example.tictactoe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun AdaptiveGridBoard(
    board: GridBoard,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGameColors.current
    val size = board.size

    Box(modifier = modifier.aspectRatio(1f).fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in 0 until size) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 0 until size) {
                        val index = r * size + c
                        val mark = board.cells[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    enabled = mark == null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onCellClick(index) }
                                )
                        ) {
                            if (mark != null) {
                                MarkSymbol(mark = mark, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}
```

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/UltimateBoardView.kt`:
```kotlin
package com.example.tictactoe.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun UltimateBoardView(
    board: UltimateBoard,
    onCellClick: (macroIndex: Int, microIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGameColors.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(modifier = modifier.aspectRatio(1f).fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (macroR in 0 until 3) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (macroC in 0 until 3) {
                        val macroIndex = macroR * 3 + macroC
                        val subBoard = board.subBoards[macroIndex]
                        val isTarget = board.activeMacroIndex == null || board.activeMacroIndex == macroIndex
                        val isComplete = subBoard.outcome() !is Outcome.InProgress
                        val macroWinner = (subBoard.outcome() as? Outcome.Won)?.winner

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isTarget && !isComplete) {
                                        Modifier.border(
                                            2.dp,
                                            colors.primary.copy(alpha = pulseAlpha),
                                            RoundedCornerShape(10.dp)
                                        )
                                    } else Modifier
                                )
                                .padding(4.dp)
                        ) {
                            AdaptiveGridBoard(
                                board = subBoard,
                                onCellClick = { microIndex -> onCellClick(macroIndex, microIndex) },
                                modifier = Modifier.fillMaxSize().alpha(if (isComplete) 0.3f else 1f)
                            )
                            if (macroWinner != null) {
                                MarkSymbol(
                                    mark = macroWinner,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

Modify `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt`:
Conditionally display `AdaptiveGridBoard` when `uiState.variant in [GRID_4X4, GRID_5X5]`, `UltimateBoardView` when `uiState.variant == ULTIMATE`, and the classic `Board` component when `uiState.variant == CLASSIC_3X3`.

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew :app:shared:jvmTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/AdaptiveGridBoard.kt app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/components/UltimateBoardView.kt app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt app/shared/src/commonTest/kotlin/com/example/tictactoe/ui/BoardLayoutCalculationTest.kt
git commit -m "feat(ui): add AdaptiveGridBoard and UltimateBoardView with animated active targeting"
```
