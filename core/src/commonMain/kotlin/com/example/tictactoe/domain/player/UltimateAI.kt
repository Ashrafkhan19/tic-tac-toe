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
                    else -> {}
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
                else -> {}
            }
        }

        // Penalty for giving opponent free choice
        if (board.activeMacroIndex == null) {
            score -= 40
        }

        return score
    }
}
