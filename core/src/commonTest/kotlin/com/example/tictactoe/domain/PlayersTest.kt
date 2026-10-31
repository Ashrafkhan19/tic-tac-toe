package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.player.ComputerPlayer
import com.example.tictactoe.domain.player.DefaultComputerPlayerProvider
import com.example.tictactoe.domain.player.MinimaxPlayer
import com.example.tictactoe.domain.player.MistakeProneComputerPlayer
import com.example.tictactoe.domain.player.MixedPlayer
import com.example.tictactoe.domain.player.RandomPlayer
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class PlayersTest {
    private fun play(vararg moves: Int) = moves.fold(GameState()) { s, m -> s.play(m) }
    private val minimax = MinimaxPlayer()

    @Test
    fun minimaxTakesWinningMove() {
        assertEquals(2, minimax.chooseMove(play(0, 3, 1, 4)))
    }

    @Test
    fun minimaxBlocksOpponent() {
        assertEquals(2, minimax.chooseMove(play(0, 4, 1)))
    }

    @Test
    fun minimaxNeverLosesAgainstAnyHuman() {
        fun explore(state: GameState) {
            when (val o = state.outcome) {
                is Outcome.Won -> assertNotEquals(Mark.X, o.winner)
                Outcome.Draw -> Unit
                Outcome.InProgress ->
                    if (state.turn == Mark.X) state.board.emptyCells.forEach { explore(state.play(it)) }
                    else explore(state.play(minimax.chooseMove(state)))
            }
        }
        explore(GameState())
    }

    @Test
    fun randomPlayerAlwaysPlaysLegalMoves() {
        val player = RandomPlayer(Random(42))
        repeat(200) {
            var s = GameState()
            while (!s.isOver) {
                val m = player.chooseMove(s)
                assertEquals(null, s.board.cells[m])
                s = s.play(m)
            }
        }
    }

    @Test
    fun mixedPlayerDelegatesByFrequency() {
        val strong = ComputerPlayer { 100 }
        val weak = ComputerPlayer { 200 }
        val always = MixedPlayer(strong, weak, strongOneIn = 1)
        assertEquals(100, always.chooseMove(GameState()))
        val moves = MixedPlayer(strong, weak, strongOneIn = 4, random = Random(1))
            .let { p -> List(400) { p.chooseMove(GameState()) } }
        val strongShare = moves.count { it == 100 } / 400.0
        assertEquals(true, strongShare in 0.15..0.35, "strong share was $strongShare")
    }

    @Test
    fun mixedPlayerRejectsInvalidFrequency() {
        assertFailsWith<IllegalArgumentException> { MixedPlayer(MinimaxPlayer(), RandomPlayer(), strongOneIn = 0) }
    }

    @Test
    fun providerRequiresAPlayerForEveryDifficulty() {
        assertFailsWith<IllegalArgumentException> {
            DefaultComputerPlayerProvider(mapOf(Difficulty.Easy to RandomPlayer()))
        }
        val provider = DefaultComputerPlayerProvider()
        Difficulty.entries.forEach { provider.playerFor(it) }
    }

    private class FixedRandom(private val value: Int) : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int = value
    }

    @Test
    fun mistakeProneUsesWeakWhenRandomHitsZero() {
        val player = MistakeProneComputerPlayer(ComputerPlayer { 100 }, ComputerPlayer { 200 }, mistakeOneIn = 4, random = FixedRandom(0))
        assertEquals(200, player.chooseMove(GameState()))
    }

    @Test
    fun mistakeProneUsesStrongOtherwise() {
        val player = MistakeProneComputerPlayer(ComputerPlayer { 100 }, ComputerPlayer { 200 }, mistakeOneIn = 4, random = FixedRandom(1))
        assertEquals(100, player.chooseMove(GameState()))
    }

    @Test
    fun mistakeOneInBelowOneRejected() {
        assertFailsWith<IllegalArgumentException> {
            MistakeProneComputerPlayer(MinimaxPlayer(), RandomPlayer(), mistakeOneIn = 0)
        }
    }

    @Test
    fun providerHasPlayerForMedium() {
        val provider = DefaultComputerPlayerProvider()
        val medium = provider.playerFor(Difficulty.Medium)
        assertNotSame(provider.playerFor(Difficulty.Unbeatable), medium)
        assertNotSame(provider.playerFor(Difficulty.Easy), medium)
    }
}
