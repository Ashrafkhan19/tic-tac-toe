package com.example.tictactoe.domain.challenge

import com.example.tictactoe.domain.model.Board
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

data class DailyChallenge(val date: LocalDate, val game: GameState, val movesToWin: Int)

/**
 * Picks the same puzzle for a given date on every device: X to move with a forced win in the
 * weekday's number of moves. Selection uses only integer arithmetic on the epoch day, so it does
 * not depend on any platform random number generator. Open only so tests can substitute a failing generator.
 */
open class DailyChallengeGenerator {
    open fun forDate(date: LocalDate): DailyChallenge {
        val movesToWin = effectiveTarget(weekdayTarget(date.dayOfWeek))
        val pool = Pools.byMovesToWin.getValue(movesToWin)
        val index = mix(date.toEpochDays()).mod(pool.size.toLong()).toInt()
        return DailyChallenge(date, GameState(pool[index], Mark.X), movesToWin)
    }

    /** Falls back to the nearest lower target with candidates (none are empty today). */
    private fun effectiveTarget(target: Int): Int =
        (target downTo 1).first { Pools.byMovesToWin[it].orEmpty().isNotEmpty() }

    internal companion object {
        fun weekdayTarget(day: DayOfWeek): Int = when (day) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> 2
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 3
        }

        /** SplitMix64 finaliser: spreads consecutive days across the pool. Pure Long arithmetic. */
        fun mix(seed: Long): Long {
            var z = seed + -0x61c8864680b583ebL
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }
    }
}

/** Candidate positions grouped by forced-win length, each sorted by a fixed board encoding. */
internal object Pools {
    private val PLACED_MARKS = setOf(2, 4, 6)

    val byMovesToWin: Map<Int, List<Board>> by lazy { build() }

    private fun build(): Map<Int, List<Board>> {
        val candidates = HashSet<Board>()
        collect(GameState(), HashSet(), candidates)
        val solver = ForcedWinSolver()
        return candidates
            .mapNotNull { board -> solver.movesToWin(board)?.let { it to board } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, boards) -> boards.sortedBy(::encode) }
    }

    /** Every unfinished position reachable by legal play with X to move and 2, 4 or 6 marks. */
    private fun collect(game: GameState, seen: HashSet<Board>, out: HashSet<Board>) {
        if (game.isOver || !seen.add(game.board)) return
        val placed = Board.CELL_COUNT - game.board.emptyCells.size
        if (game.turn == Mark.X && placed in PLACED_MARKS) out += game.board
        if (placed >= PLACED_MARKS.max()) return
        for (cell in game.board.emptyCells) collect(game.play(cell), seen, out)
    }

    /** Base-3 code (empty 0, X 1, O 2), cell 0 most significant. */
    fun encode(board: Board): Int = board.cells.fold(0) { acc, mark ->
        acc * 3 + when (mark) {
            null -> 0
            Mark.X -> 1
            Mark.O -> 2
        }
    }
}
