package com.example.tictactoe.domain.player

import com.example.tictactoe.domain.model.Board
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import kotlin.random.Random

/** Plays a uniformly random legal move. */
class RandomPlayer(private val random: Random = Random) : ComputerPlayer {
    override fun chooseMove(game: GameState): Int = game.board.emptyCells.random(random)
}

/** Perfect play via exhaustive minimax; never loses. */
class MinimaxPlayer : ComputerPlayer {
    override fun chooseMove(game: GameState): Int {
        val moves = game.board.emptyCells
        // Any opening is equally good on an empty board; take the centre.
        if (moves.size == Board.CELL_COUNT) return 4
        return moves.maxBy { score(game.play(it), game.turn, depth = 1) }
    }

    /** Positive when [me] wins, negative when they lose; faster wins and slower losses score higher. */
    private fun score(game: GameState, me: Mark, depth: Int): Int =
        when (val outcome = game.outcome) {
            is Outcome.Won if outcome.winner == me -> 10 - depth
            is Outcome.Won -> depth - 10
            Outcome.Draw -> 0
            Outcome.InProgress -> {
                val scores = game.board.emptyCells.map { score(game.play(it), me, depth + 1) }
                if (game.turn == me) scores.max() else scores.min()
            }
        }
}

/** Uses [strong] one time in [strongOneIn], otherwise [weak]. */
class MixedPlayer(
    private val strong: ComputerPlayer,
    private val weak: ComputerPlayer,
    private val strongOneIn: Int,
    private val random: Random = Random,
) : ComputerPlayer {
    init {
        require(strongOneIn >= 1) { "strongOneIn must be at least 1" }
    }

    override fun chooseMove(game: GameState): Int =
        if (random.nextInt(strongOneIn) == 0) strong.chooseMove(game) else weak.chooseMove(game)
}

/** Uses [weak] one time in [mistakeOneIn], otherwise [strong]. */
class MistakeProneComputerPlayer(
    private val strong: ComputerPlayer,
    private val weak: ComputerPlayer,
    private val mistakeOneIn: Int,
    private val random: Random = Random,
) : ComputerPlayer {
    init {
        require(mistakeOneIn >= 1) { "mistakeOneIn must be at least 1" }
    }

    override fun chooseMove(game: GameState): Int =
        if (random.nextInt(mistakeOneIn) == 0) weak.chooseMove(game) else strong.chooseMove(game)
}

/**
 * Default difficulty table. New difficulties are added by registering a player;
 * callers keep depending on [ComputerPlayerProvider] only.
 */
class DefaultComputerPlayerProvider(
    private val players: Map<Difficulty, ComputerPlayer> = defaultPlayers(),
) : ComputerPlayerProvider {
    init {
        require(players.keys.containsAll(Difficulty.entries)) { "Every difficulty needs a player" }
    }

    override fun playerFor(difficulty: Difficulty): ComputerPlayer = players.getValue(difficulty)

    private companion object {
        fun defaultPlayers(): Map<Difficulty, ComputerPlayer> {
            val perfect = MinimaxPlayer()
            return mapOf(
                Difficulty.Easy to MixedPlayer(strong = perfect, weak = RandomPlayer(), strongOneIn = 4),
                Difficulty.Medium to MistakeProneComputerPlayer(perfect, RandomPlayer(), mistakeOneIn = 4),
                Difficulty.Unbeatable to perfect,
            )
        }
    }
}
