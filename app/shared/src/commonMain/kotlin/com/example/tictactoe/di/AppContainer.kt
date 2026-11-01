package com.example.tictactoe.di

import com.example.tictactoe.ads.AdsManager
import com.example.tictactoe.ads.NoOpAdsManager
import com.example.tictactoe.data.SettingsProgressRepository
import com.example.tictactoe.domain.challenge.DailyChallengeGenerator
import com.example.tictactoe.domain.player.ComputerPlayerProvider
import com.example.tictactoe.domain.player.DefaultComputerPlayerProvider
import com.example.tictactoe.domain.progress.InMemoryProgressRepository
import com.example.tictactoe.domain.progress.ProgressRepository
import com.example.tictactoe.domain.time.SystemTodayProvider
import com.example.tictactoe.domain.time.TodayProvider
import com.example.tictactoe.domain.usecase.ApplyComputerMove
import com.example.tictactoe.domain.usecase.ChangeSettings
import com.example.tictactoe.domain.usecase.ChooseComputerMove
import com.example.tictactoe.domain.usecase.CompleteDailyChallenge
import com.example.tictactoe.domain.usecase.PlayHumanMove
import com.example.tictactoe.domain.usecase.RecordGameResult
import com.example.tictactoe.domain.usecase.ResetScores
import com.example.tictactoe.domain.usecase.StartNextRound
import com.example.tictactoe.presentation.GameReducer
import com.example.tictactoe.presentation.GameViewModel
import com.russhwolf.settings.Settings

private fun defaultProgressRepository(): ProgressRepository = try {
    SettingsProgressRepository(Settings())
} catch (t: Throwable) {
    InMemoryProgressRepository()
}

/**
 * Composition root: the only place that knows which concrete classes implement the domain
 * interfaces. Swap [computerPlayers] to change how the computer plays, [progressRepository] to
 * change where progress is stored and [todayProvider] to change the clock.
 */
class AppContainer(
    private val computerPlayers: ComputerPlayerProvider = DefaultComputerPlayerProvider(),
    private val progressRepository: ProgressRepository = defaultProgressRepository(),
    private val todayProvider: TodayProvider = SystemTodayProvider(),
    val adsManager: AdsManager = NoOpAdsManager(),
) {
    fun createGameViewModel(): GameViewModel = GameViewModel(
        reducer = GameReducer(
            playHumanMove = PlayHumanMove(),
            applyComputerMove = ApplyComputerMove(),
            startNextRound = StartNextRound(),
            changeSettings = ChangeSettings(),
            resetScores = ResetScores(),
        ),
        chooseComputerMove = ChooseComputerMove(computerPlayers),
        repository = progressRepository,
        recordGameResult = RecordGameResult(),
        completeDailyChallenge = CompleteDailyChallenge(),
        today = todayProvider,
        challenges = DailyChallengeGenerator(),
    )
}
