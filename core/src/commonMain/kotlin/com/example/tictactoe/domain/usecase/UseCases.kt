package com.example.tictactoe.domain.usecase

import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.player.ComputerPlayerProvider
import com.example.tictactoe.domain.progress.DifficultyStats
import com.example.tictactoe.domain.progress.GameResult
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.XpRules
import com.example.tictactoe.domain.progress.completed
import kotlinx.datetime.LocalDate

/** A human taps a cell. Ignored while it is the computer's turn. */
class PlayHumanMove {
    operator fun invoke(match: Match, index: Int): Match =
        if (match.isComputerTurn) match else match.play(index)
}

/** Picks (but does not apply) the computer's move for the current position. */
class ChooseComputerMove(private val players: ComputerPlayerProvider) {
    operator fun invoke(match: Match): Int =
        players.playerFor(match.settings.difficulty).chooseMove(match.game)
}

/** Applies a previously chosen computer move. Ignored unless it is the computer's turn. */
class ApplyComputerMove {
    operator fun invoke(match: Match, index: Int): Match =
        if (match.isComputerTurn) match.play(index) else match
}

class StartNextRound {
    operator fun invoke(match: Match): Match = match.nextRound()
}

class ChangeSettings {
    operator fun invoke(match: Match, settings: MatchSettings): Match = match.withSettings(settings)
}

class ResetScores {
    operator fun invoke(match: Match): Match = match.withSettings(match.settings)
}

/** Folds a finished vs-Computer game into the player's progress. The human is X. */
class RecordGameResult {
    operator fun invoke(progress: PlayerProgress, settings: MatchSettings, outcome: Outcome): PlayerProgress {
        if (settings.mode != Mode.VsComputer) return progress
        val result = when (outcome) {
            Outcome.InProgress -> return progress
            Outcome.Draw -> GameResult.Draw
            is Outcome.Won -> if (outcome.winner == Mark.X) GameResult.Win else GameResult.Loss
        }
        return progress.recordGame(settings.variant, settings.difficulty, result)
    }
}

/** Marks the daily challenge for [challengeDate] complete: advances the streak and awards the bonus once. */
class CompleteDailyChallenge {
    operator fun invoke(progress: PlayerProgress, challengeDate: LocalDate): PlayerProgress {
        val last = progress.streak.lastCompleted
        if (last != null && last >= challengeDate) return progress
        return progress.copy(
            xp = progress.xp + XpRules.CHALLENGE_BONUS,
            streak = progress.streak.completed(challengeDate),
        )
    }
}
