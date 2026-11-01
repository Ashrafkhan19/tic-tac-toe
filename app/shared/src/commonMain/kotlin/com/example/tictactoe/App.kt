package com.example.tictactoe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tictactoe.ads.LocalAdsManager
import com.example.tictactoe.di.AppContainer
import com.example.tictactoe.domain.focus.DailyCircuitState
import com.example.tictactoe.domain.focus.FocusXpRules
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.ui.GameContent
import com.example.tictactoe.ui.focus.FocusTimerScreen
import com.example.tictactoe.ui.focus.GymScreen
import com.example.tictactoe.ui.focus.NeuroLabScreen
import com.example.tictactoe.ui.focus.ReadingSanctuaryScreen
import com.example.tictactoe.ui.focus.SomaticResetScreen
import com.example.tictactoe.ui.focus.UrgeSurferScreen
import com.example.tictactoe.ui.theme.LocalGameColors
import com.example.tictactoe.ui.theme.TicTacToeTheme

enum class ScreenRoute {
    GYM,
    URGE_SURFER,
    NEURO_LAB,
    DEEP_READING,
    FOCUS_TIMER,
    STRATEGY_GAME,
    SOMATIC_RESET
}

@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    CompositionLocalProvider(LocalAdsManager provides container.adsManager) {
        TicTacToeTheme {
            var currentRoute by remember { mutableStateOf(ScreenRoute.GYM) }
            var playerProgress by remember { mutableStateOf(container.progressRepository.load()) }
            var dailyCircuit by remember { mutableStateOf(DailyCircuitState(dateIso = "today")) }

            when (currentRoute) {
                ScreenRoute.GYM -> {
                    GymScreen(
                        progress = playerProgress,
                        dailyCircuit = dailyCircuit,
                        onOpenUrgeSurfer = { currentRoute = ScreenRoute.URGE_SURFER },
                        onOpenNeuroLab = { currentRoute = ScreenRoute.NEURO_LAB },
                        onOpenReading = { currentRoute = ScreenRoute.DEEP_READING },
                        onOpenFocusTimer = { currentRoute = ScreenRoute.FOCUS_TIMER },
                        onOpenStrategyGame = { currentRoute = ScreenRoute.STRATEGY_GAME },
                        onOpenSomaticReset = { currentRoute = ScreenRoute.SOMATIC_RESET },
                        onToggleSleepFoundation = {
                            val nextState = !dailyCircuit.sleepLogged
                            dailyCircuit = dailyCircuit.copy(sleepLogged = nextState)
                            if (nextState) {
                                val updated = playerProgress.copy(
                                    xp = playerProgress.xp + FocusXpRules.xpForDailyFoundation(),
                                    sleepLogsCount = playerProgress.sleepLogsCount + 1
                                )
                                container.progressRepository.save(updated)
                                playerProgress = updated
                            }
                        },
                        onToggleMovementFoundation = {
                            val nextState = !dailyCircuit.movementLogged
                            dailyCircuit = dailyCircuit.copy(movementLogged = nextState)
                            if (nextState) {
                                val updated = playerProgress.copy(
                                    xp = playerProgress.xp + FocusXpRules.xpForDailyFoundation(),
                                    movementLogsCount = playerProgress.movementLogsCount + 1
                                )
                                container.progressRepository.save(updated)
                                playerProgress = updated
                            }
                        }
                    )
                }
                ScreenRoute.URGE_SURFER -> {
                    UrgeSurferScreen(
                        onFinished = { xp ->
                            val updated = playerProgress.copy(
                                xp = playerProgress.xp + xp,
                                urgesSurfedCount = playerProgress.urgesSurfedCount + 1
                            )
                            container.progressRepository.save(updated)
                            playerProgress = updated
                            dailyCircuit = dailyCircuit.copy(urgeWarmupCompleted = true)
                            currentRoute = ScreenRoute.GYM
                        },
                        onDismiss = { currentRoute = ScreenRoute.GYM }
                    )
                }
                ScreenRoute.NEURO_LAB -> {
                    NeuroLabScreen(
                        onDrillComplete = { score ->
                            val xp = if (score >= 80) 35 else 20
                            val updated = playerProgress.copy(xp = playerProgress.xp + xp)
                            container.progressRepository.save(updated)
                            playerProgress = updated
                            dailyCircuit = dailyCircuit.copy(neuroDrillCompleted = true)
                            currentRoute = ScreenRoute.GYM
                        },
                        onBack = { currentRoute = ScreenRoute.GYM }
                    )
                }
                ScreenRoute.DEEP_READING -> {
                    ReadingSanctuaryScreen(
                        onArticleFinished = { _, score ->
                            val xp = FocusXpRules.xpForDeepReading(score)
                            val updated = playerProgress.copy(
                                xp = playerProgress.xp + xp,
                                totalFocusMinutes = playerProgress.totalFocusMinutes + 5
                            )
                            container.progressRepository.save(updated)
                            playerProgress = updated
                            dailyCircuit = dailyCircuit.copy(deepActivityCompleted = true)
                            currentRoute = ScreenRoute.GYM
                        },
                        onBack = { currentRoute = ScreenRoute.GYM }
                    )
                }
                ScreenRoute.FOCUS_TIMER -> {
                    FocusTimerScreen(
                        onSessionComplete = { durationMinutes, distractions ->
                            val xp = FocusXpRules.xpForFocusSession(durationMinutes, distractions)
                            val updated = playerProgress.copy(
                                xp = playerProgress.xp + xp,
                                totalFocusMinutes = playerProgress.totalFocusMinutes + durationMinutes
                            )
                            container.progressRepository.save(updated)
                            playerProgress = updated
                            dailyCircuit = dailyCircuit.copy(deepActivityCompleted = true)
                            currentRoute = ScreenRoute.GYM
                        },
                        onBack = { currentRoute = ScreenRoute.GYM }
                    )
                }
                ScreenRoute.STRATEGY_GAME -> {
                    GameScreen(
                        container = container,
                        onBackToGym = {
                            playerProgress = container.progressRepository.load()
                            currentRoute = ScreenRoute.GYM
                        }
                    )
                }
                ScreenRoute.SOMATIC_RESET -> {
                    SomaticResetScreen(
                        onFinished = { xp ->
                            val updated = playerProgress.copy(xp = playerProgress.xp + xp)
                            container.progressRepository.save(updated)
                            playerProgress = updated
                            currentRoute = ScreenRoute.GYM
                        },
                        onDismiss = { currentRoute = ScreenRoute.GYM }
                    )
                }
            }
        }
    }
}

/** Wiring only: owns the ViewModel and hands plain state and a callback to [GameContent]. */
@Composable
private fun GameScreen(container: AppContainer, onBackToGym: () -> Unit) {
    val colors = LocalGameColors.current
    val viewModel = viewModel { container.createGameViewModel() }
    val ui by viewModel.state.collectAsStateWithLifecycle()
    // Picks up a midnight rollover when the app returns to the foreground.
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(Intent.RefreshDate)
        onPauseOrDispose { }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        GameContent(ui = ui, onIntent = viewModel::onIntent)
        OutlinedButton(
            onClick = onBackToGym,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .height(34.dp)
        ) {
            Text("← Mind Gym", color = colors.onSurface, fontSize = 12.sp)
        }
    }
}
