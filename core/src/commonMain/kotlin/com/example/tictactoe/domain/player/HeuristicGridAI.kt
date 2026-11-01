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
                    else -> {}
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
