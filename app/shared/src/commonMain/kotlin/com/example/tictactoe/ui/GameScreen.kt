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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.challenge.DailyChallengeGenerator
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameMove
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Match
import com.example.tictactoe.domain.model.MatchSettings
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.StreakState
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.presentation.UiState
import com.example.tictactoe.ui.components.ActionButtons
import com.example.tictactoe.ui.components.AdaptiveGridBoard
import com.example.tictactoe.ui.components.AnimatedBackground
import com.example.tictactoe.ui.components.Board
import com.example.tictactoe.ui.components.ChallengeHeader
import com.example.tictactoe.ui.components.Confetti
import com.example.tictactoe.ui.components.DailyChallengeBanner
import com.example.tictactoe.ui.components.GlassPanel
import com.example.tictactoe.ui.components.ModeSelector
import com.example.tictactoe.ui.components.ProgressStrip
import com.example.tictactoe.ui.components.Scoreboard
import com.example.tictactoe.ui.components.StatusBanner
import com.example.tictactoe.ui.components.UltimateBoardView
import com.example.tictactoe.ui.components.VariantBadge
import com.example.tictactoe.ui.components.VariantPickerSheet
import com.example.tictactoe.ui.theme.LocalGameColors
import com.example.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.datetime.LocalDate

private val WideBreakpoint = 720.dp
private val MaxBoardSize = 620.dp

private val CompactGutter = 16.dp
private val CompactMaxWidth = 520.dp
private val CompactSpacing = 10.dp

/** Padding, borders and gaps above the board in [CompactLayout] (independent of text size). */
private val CompactFixedHeight = 135.dp

/** Line heights of the text above the board in [CompactLayout] at font scale 1 (title, strip, banner, scores, status). */
private val CompactTextHeight = 160.dp

/** Plain, previewable screen: immutable state in, intents out. Adapts between a stacked and a side-by-side layout. */
@Composable
internal fun GameContent(ui: UiState, onIntent: (Intent) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val outcome = ui.game.outcome
    val vsCpu = ui.mode == Mode.VsComputer

    var showVariantPicker by remember { mutableStateOf(false) }
    val onVariantClick = { showVariantPicker = true }

    if (showVariantPicker) {
        VariantPickerSheet(
            selectedVariant = ui.variant,
            onSelect = { variant ->
                onIntent(Intent.SelectVariant(variant))
            },
            onDismiss = { showVariantPicker = false },
        )
    }

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
                WideLayout(ui, onIntent, boardModifier, onVariantClick)
            } else {
                val boardSize = minOf(maxWidth - CompactGutter * 2, CompactMaxWidth)
                val fontScale = LocalDensity.current.fontScale
                // Rough height of everything stacked above the board: fixed padding plus text that grows with the font scale.
                val aboveBoard = CompactFixedHeight + CompactTextHeight * fontScale
                CompactLayout(ui, onIntent, boardModifier, boardFirst = boardSize + aboveBoard > maxHeight, onVariantClick = onVariantClick)
            }
        }
        Confetti(confettiTrigger, listOf(colors.x, colors.o, colors.glowA, colors.glowB, colors.onSurface), Modifier.fillMaxSize())
    }
}

/**
 * Phone layout. At the default text size everything above the board is a few slim rows, so the whole
 * board is on screen on a 667pt phone. When the text is too large for that, [boardFirst] moves the
 * board to the top (the title is dropped) and the rest follows below it.
 */
@Composable
private fun CompactLayout(
    ui: UiState,
    onIntent: (Intent) -> Unit,
    boardModifier: Modifier,
    boardFirst: Boolean,
    onVariantClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = CompactGutter, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = CompactMaxWidth),
            verticalArrangement = Arrangement.spacedBy(CompactSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val board = @Composable {
                GameBoard(
                    ui = ui,
                    onIntent = onIntent,
                    modifier = boardModifier.fillMaxWidth(),
                )
            }
            if (boardFirst) {
                VariantBadge(
                    variant = ui.variant,
                    enabled = !ui.inChallenge,
                    onClick = onVariantClick,
                )
                StatusBanner(ui)
                board()
                Scoreboard(ui)
                ProgressSection(ui, onIntent)
            } else {
                Header(ui = ui, onVariantClick = onVariantClick, compact = true)
                ProgressSection(ui, onIntent)
                Scoreboard(ui)
                StatusBanner(ui)
                board()
            }
            GlassPanel(Modifier.fillMaxWidth()) {
                if (ui.inChallenge) ChallengeHeader(ui, onIntent) else ModeSelector(ui, onIntent)
                ActionButtons(ui, onIntent)
            }
        }
    }
}

@Composable
private fun GameBoard(
    ui: UiState,
    onIntent: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (ui.variant) {
        GameVariant.CLASSIC_3X3 -> {
            Board(
                game = ui.game,
                enabled = !ui.isComputerTurn,
                onCellClick = { onIntent(Intent.CellTapped(it)) },
                modifier = modifier,
            )
        }
        GameVariant.GRID_4X4, GameVariant.GRID_5X5 -> {
            val grid = ui.gridBoard ?: GridBoard(
                size = if (ui.variant == GameVariant.GRID_5X5) 5 else 4,
                winLength = 4,
            )
            AdaptiveGridBoard(
                board = grid,
                enabled = !ui.isComputerTurn,
                onCellClick = { onIntent(Intent.PlaceVariantMove(GameMove.Grid(it))) },
                modifier = modifier,
            )
        }
        GameVariant.ULTIMATE -> {
            val ultimate = ui.ultimateBoard ?: UltimateBoard()
            UltimateBoardView(
                board = ultimate,
                enabled = !ui.isComputerTurn,
                onCellClick = { macro, micro ->
                    onIntent(Intent.PlaceVariantMove(GameMove.Ultimate(macro, micro)))
                },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun WideLayout(
    ui: UiState,
    onIntent: (Intent) -> Unit,
    boardModifier: Modifier,
    onVariantClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1.1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            GameBoard(
                ui = ui,
                onIntent = onIntent,
                modifier = boardModifier.widthIn(max = MaxBoardSize).heightIn(max = MaxBoardSize),
            )
        }
        Column(
            Modifier.weight(1f).widthIn(max = 460.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Header(ui = ui, onVariantClick = onVariantClick)
            ProgressSection(ui, onIntent)
            StatusBanner(ui)
            Scoreboard(ui)
            GlassPanel(Modifier.fillMaxWidth()) {
                if (ui.inChallenge) ChallengeHeader(ui, onIntent) else ModeSelector(ui, onIntent)
                ActionButtons(ui, onIntent)
            }
        }
    }
}

/** Streak/XP strip and daily-challenge entry. Hidden inside a challenge, where `today` may be stale and urgency would mislead. */
@Composable
private fun ProgressSection(ui: UiState, onIntent: (Intent) -> Unit) {
    if (ui.inChallenge) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CompactSpacing)) {
        ProgressStrip(ui)
        DailyChallengeBanner(ui, onIntent)
    }
}

/** Title and variant badge. [compact] drops the tagline and uses a smaller style to leave room for the board on phones. */
@Composable
private fun Header(
    ui: UiState,
    onVariantClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalGameColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Tic Tac Toe",
            style = (if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall).copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.linearGradient(listOf(colors.x, colors.o)),
            ),
        )
        if (!compact) {
            val tagline = when (ui.variant) {
                GameVariant.CLASSIC_3X3 -> "Three in a row wins"
                GameVariant.GRID_4X4 -> "Four in a row wins on 4×4"
                GameVariant.GRID_5X5 -> "Four in a row wins on 5×5"
                GameVariant.ULTIMATE -> "Win three small boards in a row"
            }
            Text(tagline, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        VariantBadge(
            variant = ui.variant,
            enabled = !ui.inChallenge,
            onClick = onVariantClick,
        )
    }
}

private val PreviewDay = LocalDate(2026, 11, 2)
private fun previewChallenge() = DailyChallengeGenerator().forDate(PreviewDay)
private fun previewGame() = Match(game = GameState().play(4).play(0).play(8))

@Preview
@Composable
private fun GameContentPreview() {
    TicTacToeTheme {
        GameContent(UiState(previewGame(), today = PreviewDay), onIntent = {})
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun PhoneAtRiskPreview() {
    TicTacToeTheme {
        GameContent(
            UiState(
                previewGame(), today = PreviewDay, todayChallenge = previewChallenge(),
                progress = PlayerProgress(xp = 230, streak = StreakState(current = 5, best = 5, lastCompleted = LocalDate(2026, 11, 1))),
            ),
            onIntent = {},
        )
    }
}

@Preview(widthDp = 800, heightDp = 1180)
@Composable
private fun TabletPortraitDonePreview() {
    TicTacToeTheme {
        GameContent(
            UiState(
                previewGame(), today = PreviewDay, todayChallenge = previewChallenge(),
                progress = PlayerProgress(xp = 340, streak = StreakState(current = 6, best = 6, lastCompleted = PreviewDay)),
            ),
            onIntent = {},
        )
    }
}

@Preview(widthDp = 1180, heightDp = 800)
@Composable
private fun TabletLandscapeLoadingPreview() {
    TicTacToeTheme {
        GameContent(UiState(previewGame(), today = PreviewDay, todayChallenge = null), onIntent = {})
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun InChallengePreview() {
    TicTacToeTheme {
        GameContent(
            UiState(
                Match(MatchSettings(Mode.VsComputer, Difficulty.Unbeatable), game = previewChallenge().game),
                today = PreviewDay, todayChallenge = previewChallenge(), inChallenge = true,
            ),
            onIntent = {},
        )
    }
}
