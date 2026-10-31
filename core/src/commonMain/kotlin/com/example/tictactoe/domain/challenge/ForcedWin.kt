package com.example.tictactoe.domain.challenge

import com.example.tictactoe.domain.model.Board
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome

/**
 * With X to move, the number of X moves until X wins when X plays fastest and O delays longest.
 * `null` if O can avoid losing, if the game is over, or if it is not X's turn.
 */
fun movesToForcedWin(game: GameState): Int? {
    if (game.isOver || game.turn != Mark.X) return null
    return ForcedWinSolver().movesToWin(game.board)
}

/** Memoised forced-win search. Not thread-safe; keep each instance confined to one caller. */
internal class ForcedWinSolver {
    private val memo = HashMap<Board, Int>()

    /** [board] is unfinished with X to move. */
    fun movesToWin(board: Board): Int? {
        val cached = memo.getOrPut(board) { solve(board) ?: NO_WIN }
        return cached.takeIf { it != NO_WIN }
    }

    private fun solve(board: Board): Int? {
        var best: Int? = null
        for (x in board.emptyCells) {
            val afterX = board.place(x, Mark.X)
            when (afterX.outcome()) {
                is Outcome.Won -> return 1
                Outcome.Draw -> continue
                Outcome.InProgress -> {
                    val worst = slowestWinAgainstBestDefence(afterX) ?: continue
                    if (best == null || worst < best) best = worst
                }
            }
        }
        return best
    }

    /** O to move on [board]; X's move count if every O reply still loses, else `null`. */
    private fun slowestWinAgainstBestDefence(board: Board): Int? {
        var worst = 0
        for (o in board.emptyCells) {
            val afterO = board.place(o, Mark.O)
            if (afterO.outcome() != Outcome.InProgress) return null
            val rest = movesToWin(afterO) ?: return null
            worst = maxOf(worst, rest + 1)
        }
        return worst
    }

    private companion object {
        const val NO_WIN = -1
    }
}
