package com.example.tictactoe.domain

import com.example.tictactoe.domain.challenge.DailyChallengeGenerator
import com.example.tictactoe.domain.challenge.Pools
import com.example.tictactoe.domain.challenge.movesToForcedWin
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyChallengeTest {
    private fun play(vararg moves: Int): GameState =
        moves.fold(GameState()) { game, move -> game.play(move) }

    private val generator = DailyChallengeGenerator()
    private val start = LocalDate(2026, 11, 1)
    private val dates = (0 until 14).map { start.plus(DatePeriod(days = it)) }

    private fun target(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> 2
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 3
    }

    @Test
    fun immediateWinIsOneMove() {
        assertEquals(1, movesToForcedWin(play(0, 3, 1, 4)))
    }

    @Test
    fun emptyBoardHasNoForcedWin() {
        assertNull(movesToForcedWin(GameState()))
    }

    @Test
    fun finishedGameHasNoForcedWin() {
        assertNull(movesToForcedWin(play(0, 3, 1, 4, 2)))
    }

    @Test
    fun challengesForTwoWeeksAreValidAndMatchWeekdayTargets() {
        for (date in dates) {
            val challenge = generator.forDate(date)
            val game = challenge.game
            assertEquals(date, challenge.date)
            assertEquals(Mark.X, game.turn, "turn on $date")
            assertFalse(game.isOver, "game over on $date")
            assertEquals(challenge.movesToWin, movesToForcedWin(game), "forced win on $date")
            assertEquals(target(date.dayOfWeek), challenge.movesToWin, "target on $date")
        }
    }

    @Test
    fun sameDateGivesSameChallenge() {
        for (date in dates) {
            assertEquals(generator.forDate(date), generator.forDate(date))
            assertEquals(generator.forDate(date), DailyChallengeGenerator().forDate(date))
        }
    }

    @Test
    fun boardsVaryAcrossTwoWeeks() {
        val boards = dates.map { generator.forDate(it).game.board }.toSet()
        assertTrue(boards.size > 1, "all 14 boards identical")
    }

    @Test
    fun everyWeekdayTargetHasCandidates() {
        val sizes = (1..3).associateWith { Pools.byMovesToWin[it].orEmpty().size }
        assertEquals(mapOf(1 to 1356, 2 to 260, 3 to 72), sizes)
    }

    /** Pins the picks so every platform must produce exactly the JVM's boards. */
    @Test
    fun picksAreStableAcrossPlatforms() {
        val golden = listOf(15, 5715, 2382, 5160, 2393, 6596, 487, 135, 13218, 17065, 1480, 2736, 735, 17526)
        assertEquals(golden, dates.map { Pools.encode(generator.forDate(it).game.board) })
    }

    @Test
    fun forcedWinAgreesWithPlainMinimax() {
        for ((moves, boards) in Pools.byMovesToWin) {
            for (board in boards) {
                // X winning on its k-th move ends the game after 2k - 1 plies.
                assertEquals(2 * moves - 1, pliesToXWin(GameState(board, Mark.X)), "board $board")
            }
        }
    }

    /** Unmemoised reference: plies until X wins under X-fastest / O-slowest play, or null. */
    private fun pliesToXWin(game: GameState): Int? {
        val outcome = game.outcome
        if (outcome is Outcome.Won) return if (outcome.winner == Mark.X) 0 else null
        if (outcome == Outcome.Draw) return null
        val results = game.board.emptyCells.map { pliesToXWin(game.play(it))?.plus(1) }
        return if (game.turn == Mark.X) results.filterNotNull().minOrNull()
        else if (results.any { it == null }) null else results.filterNotNull().max()
    }

    @Test
    fun neverReturnsBoardWhereOHasWon() {
        for (i in 0 until 400) {
            val outcome = generator.forDate(start.plus(DatePeriod(days = i))).game.outcome
            assertFalse(outcome is Outcome.Won && outcome.winner == Mark.O)
        }
    }
}
