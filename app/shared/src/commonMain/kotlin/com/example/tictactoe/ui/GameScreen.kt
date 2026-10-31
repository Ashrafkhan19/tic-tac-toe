package com.example.tictactoe.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.presentation.UiState
import com.example.tictactoe.ui.components.ActionButtons
import com.example.tictactoe.ui.components.AnimatedBackground
import com.example.tictactoe.ui.components.Board
import com.example.tictactoe.ui.components.Confetti
import com.example.tictactoe.ui.components.GlassPanel
import com.example.tictactoe.ui.components.ModeSelector
import com.example.tictactoe.ui.components.Scoreboard
import com.example.tictactoe.ui.components.StatusBanner
import com.example.tictactoe.ui.theme.LocalGameColors
import com.example.tictactoe.ui.theme.TicTacToeTheme

private val WideBreakpoint = 720.dp
private val MaxBoardSize = 620.dp

/** Plain, previewable screen: immutable state in, intents out. Adapts between a stacked and a side-by-side layout. */
@Composable
internal fun GameContent(ui: UiState, onIntent: (Intent) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val outcome = ui.game.outcome
    val vsCpu = ui.mode == Mode.VsComputer

    // Celebrate human wins (or any win in 2-player); shake on a loss or draw.
    val celebrates = outcome is Outcome.Won && (!vsCpu || outcome.winner == Mark.X)
    val confettiTrigger = if (celebrates) ui.xWins + ui.oWins else -1
    val shakeRound: GameState? = ui.game.takeIf { outcome == Outcome.Draw || (outcome is Outcome.Won && !celebrates) }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeRound) {
        if (shakeRound != null) {
            listOf(-12f, 12f, -9f, 9f, -5f, 5f, 0f).forEach { shake.animateTo(it, tween(55)) }
        }
    }
    val boardModifier = Modifier.graphicsLayer { translationX = shake.value * density }

    AnimatedBackground(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeContentPadding()) {
            if (maxWidth >= WideBreakpoint && maxWidth > maxHeight) {
                WideLayout(ui, onIntent, boardModifier)
            } else {
                CompactLayout(ui, onIntent, boardModifier)
            }
        }
        Confetti(confettiTrigger, listOf(colors.x, colors.o, colors.glowA, colors.glowB, colors.onSurface), Modifier.fillMaxSize())
    }
}

@Composable
private fun CompactLayout(ui: UiState, onIntent: (Intent) -> Unit, boardModifier: Modifier) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Header()
            Scoreboard(ui)
            StatusBanner(ui)
            Board(ui.game, enabled = !ui.isComputerTurn, onCellClick = { onIntent(Intent.CellTapped(it)) }, modifier = boardModifier.fillMaxWidth())
            GlassPanel(Modifier.fillMaxWidth()) {
                ModeSelector(ui, onIntent)
                ActionButtons(ui, onIntent)
            }
        }
    }
}

@Composable
private fun WideLayout(ui: UiState, onIntent: (Intent) -> Unit, boardModifier: Modifier) {
    Row(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1.1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Board(
                ui.game,
                enabled = !ui.isComputerTurn,
                onCellClick = { onIntent(Intent.CellTapped(it)) },
                modifier = boardModifier.widthIn(max = MaxBoardSize).heightIn(max = MaxBoardSize),
            )
        }
        Column(
            Modifier.weight(1f).widthIn(max = 460.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Header()
            StatusBanner(ui)
            Scoreboard(ui)
            GlassPanel(Modifier.fillMaxWidth()) {
                ModeSelector(ui, onIntent)
                ActionButtons(ui, onIntent)
            }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Tic Tac Toe",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.linearGradient(listOf(colors.x, colors.o)),
            ),
        )
        Text("Three in a row wins", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
    }
}

@Preview
@Composable
private fun GameContentPreview() {
    TicTacToeTheme() {
        GameContent(UiState(Match(game = GameState().play(4).play(0).play(8))), onIntent = {})
    }
}
