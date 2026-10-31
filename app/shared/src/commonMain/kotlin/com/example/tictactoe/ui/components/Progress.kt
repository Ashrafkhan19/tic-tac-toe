package com.example.tictactoe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.challenge.DailyChallengeGenerator
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.StreakState
import com.example.tictactoe.domain.progress.XpRules
import com.example.tictactoe.domain.progress.displayed
import com.example.tictactoe.domain.progress.isAtRisk
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.presentation.UiState
import com.example.tictactoe.ui.theme.LocalGameColors
import com.example.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.datetime.LocalDate

private val FlameColor = Color(0xFFFFA94D)

/** Streak, level and XP progress. Streak urgency is only shown for a live streak that today's challenge would extend. */
@Composable
fun ProgressStrip(ui: UiState, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val streak = ui.progress.streak
    val shown = streak.displayed(ui.today)
    val atRisk = shown > 0 && streak.isAtRisk(ui.today)
    val xp = ui.progress.xp
    val level = XpRules.levelFor(xp)
    val floor = XpRules.xpToReach(level)
    val span = XpRules.xpToReach(level + 1) - floor
    val fraction = ((xp - floor).toFloat() / span).coerceIn(0f, 1f)
    val streakText = when {
        shown == 0 -> "No streak"
        shown == 1 -> "1 day streak"
        else -> "$shown day streak"
    }
    val streakDescription = if (atRisk) "$streakText, at risk. Complete today's challenge to keep it" else streakText

    GlassPanel(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = streakDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Flame(if (shown == 0) colors.muted.copy(alpha = 0.5f) else FlameColor, Modifier.size(22.dp))
                Text(
                    text = if (shown == 0) "0" else shown.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (shown == 0) colors.muted else colors.onSurface,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Text(
                    text = if (atRisk) "Keep it alive today" else if (shown == 0) "Start a streak" else "days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (atRisk) FlameColor else colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
            Text(
                "Level $level",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
                maxLines = 1,
            )
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val barShape = RoundedCornerShape(50)
            Box(
                Modifier.fillMaxWidth().height(8.dp).clip(barShape).background(colors.cell)
                    .semantics { contentDescription = "Level $level, ${xp - floor} of $span XP to level ${level + 1}" },
            ) {
                if (fraction > 0f) {
                    Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(barShape).background(colors.primary))
                }
            }
            Text(
                "${xp - floor} / $span XP",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** A simple flame drawn as a path, so it renders identically on every platform (no font glyph dependency). */
@Composable
private fun Flame(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.55f, h * 0.3f, w * 0.95f, h * 0.4f, w * 0.9f, h * 0.68f)
            cubicTo(w * 0.87f, h * 0.9f, w * 0.7f, h, w * 0.5f, h)
            cubicTo(w * 0.3f, h, w * 0.13f, h * 0.9f, w * 0.1f, h * 0.68f)
            cubicTo(w * 0.08f, h * 0.5f, w * 0.22f, h * 0.38f, w * 0.3f, h * 0.25f)
            cubicTo(w * 0.35f, h * 0.35f, w * 0.4f, h * 0.38f, w * 0.42f, h * 0.38f)
            cubicTo(w * 0.42f, h * 0.2f, w * 0.45f, h * 0.08f, w * 0.5f, 0f)
            close()
        }
        drawPath(path, color)
        drawCircle(Color.White.copy(alpha = 0.35f), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.76f))
    }
}

/**
 * Entry point to today's puzzle. Hidden by the caller while in a challenge.
 * A null [UiState.todayChallenge] is the loading state: shown disabled, never blocking the board.
 */
@Composable
fun DailyChallengeBanner(ui: UiState, onIntent: (Intent) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val challenge = ui.todayChallenge
    val done = challenge != null && ui.progress.streak.lastCompleted == challenge.date
    GlassPanel(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Daily challenge",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurface,
                )
                Text(
                    text = when {
                        challenge == null -> "Loading today's puzzle..."
                        done -> "Done today. Come back tomorrow"
                        else -> "Win in ${challenge.movesToWin}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
            if (done) {
                Text("Done today", style = MaterialTheme.typography.labelLarge, color = colors.primary, maxLines = 1)
            } else {
                Button(onClick = { onIntent(Intent.StartDailyChallenge) }, enabled = challenge != null) { Text("Play") }
            }
        }
    }
}

/** Replaces the mode selector while playing the daily challenge. */
@Composable
fun ChallengeHeader(ui: UiState, onIntent: (Intent) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Daily challenge",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface,
        )
        ui.todayChallenge?.let {
            Text("Win in ${it.movesToWin}", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
        OutlinedButton(onClick = { onIntent(Intent.LeaveDailyChallenge) }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to free play")
        }
    }
}

private val PreviewDay = LocalDate(2026, 11, 2)
private val PreviewChallenge = DailyChallengeGenerator().forDate(PreviewDay)

@Composable
private fun PreviewColumn(ui: UiState) {
    TicTacToeTheme {
        Column(Modifier.background(LocalGameColors.current.backgroundTop).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressStrip(ui)
            DailyChallengeBanner(ui, {})
            ChallengeHeader(ui, {})
        }
    }
}

@Preview
@Composable
private fun NewPlayerPreview() = PreviewColumn(UiState(today = PreviewDay, todayChallenge = PreviewChallenge))

@Preview
@Composable
private fun MidStreakAtRiskPreview() = PreviewColumn(
    UiState(
        today = PreviewDay,
        todayChallenge = PreviewChallenge,
        progress = PlayerProgress(xp = 230, streak = StreakState(current = 5, best = 5, lastCompleted = LocalDate(2026, 11, 1))),
    ),
)

@Preview
@Composable
private fun ChallengeDonePreview() = PreviewColumn(
    UiState(
        today = PreviewDay,
        todayChallenge = PreviewChallenge,
        progress = PlayerProgress(xp = 340, streak = StreakState(current = 6, best = 6, lastCompleted = PreviewDay)),
    ),
)

@Preview
@Composable
private fun LoadingBannerPreview() = PreviewColumn(UiState(today = PreviewDay, todayChallenge = null))
