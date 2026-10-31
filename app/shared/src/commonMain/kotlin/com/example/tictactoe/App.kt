package com.example.tictactoe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tictactoe.di.AppContainer
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.ui.GameContent
import com.example.tictactoe.ui.theme.TicTacToeTheme

@Composable
fun App() {
    TicTacToeTheme {
        GameScreen()
    }
}

/** Wiring only: owns the ViewModel and hands plain state and a callback to [GameContent]. */
@Composable
private fun GameScreen(container: AppContainer = remember { AppContainer() }) {
    val viewModel = viewModel { container.createGameViewModel() }
    val ui by viewModel.state.collectAsStateWithLifecycle()
    // Picks up a midnight rollover when the app returns to the foreground.
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(Intent.RefreshDate)
        onPauseOrDispose { }
    }
    GameContent(ui = ui, onIntent = viewModel::onIntent)
}
