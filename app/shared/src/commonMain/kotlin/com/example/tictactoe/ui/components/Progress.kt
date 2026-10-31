package com.example.tictactoe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
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
private val PillShape = RoundedCornerShape(50)

/** The slim translucent row shared by the progress strip and the challenge banner. */
@Composable
private fun Modifier.slimRow(): Modifier {
    val colors = LocalGameColors.current
    return fillMaxWidth()
        .clip(PillShape)
        .background(colors.panel)
        .border(1.dp, colors.panelBorder, PillShape)
}

/**
 * One slim row: streak (flame + count), level and a thin XP bar.
 * Streak urgency is only shown for a live streak that today's challenge would extend.
 */
@Composable
fun ProgressStrip(ui: UiState, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val streak = ui.progress.streak
    val shown = streak.displayed(ui.today)
    val atRisk = streak.isAtRisk(ui.today)
    val xp = ui.progress.xp
    val level = XpRules.levelFor(xp)
    val floor = XpRules.xpToReach(level)
    val span = XpRules.xpToReach(level + 1) - floor
    val fraction = ((xp - floor).toFloat() / span).coerceIn(0f, 1f)
    val streakText = when (shown) {
        0 -> "No streak"
        1 -> "1 day streak"
        else -> "$shown day streak"
    }
    val streakDescription = if (atRisk) "$streakText, at risk. Complete today's challenge to keep it" else streakText

    Row(
        modifier.slimRow().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = streakDescription },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Flame(if (shown == 0) colors.muted.copy(alpha = 0.5f) else FlameColor, Modifier.size(16.dp))
            Text(
                text = shown.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (shown == 0) colors.muted else colors.onSurface,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics {},
            )
            if (atRisk) {
                Text(
                    "Keep it alive",
                    style = MaterialTheme.typography.labelMedium,
                    color = FlameColor,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        Text(
            "Lv $level",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp).clearAndSetSemantics {},
        )
        Box(
            Modifier.weight(1f).height(6.dp).clip(PillShape).background(colors.cell)
                .semantics {
                    contentDescription = "Level $level, ${xp - floor} of $span XP to level ${level + 1}"
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                },
        ) {
            if (fraction > 0f) {
                Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(PillShape).background(colors.primary))
            }
        }
        Text(
            "${xp - floor}/$span",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
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
 * One slim, tappable row leading to today's puzzle. Hidden by the caller while in a challenge.
 * A null [UiState.todayChallenge] is the loading state: shown disabled, never blocking the board.
 */
@Composable
fun DailyChallengeBanner(ui: UiState, onIntent: (Intent) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val challenge = ui.todayChallenge
    val done = ui.dailyChallengeDone
    val detail = when {
        challenge == null -> "Loading..."
        done -> "Come back tomorrow"
        else -> "Win in ${challenge.movesToWin}"
    }
    val description = when {
        challenge == null -> "Daily challenge, loading today's puzzle"
        done -> "Daily challenge done today. Come back tomorrow"
        else -> "Daily challenge, win in ${challenge.movesToWin} moves. Play"
    }
    val playable = challenge != null && !done
    Row(
        modifier.slimRow()
            .clickable(enabled = playable, role = Role.Button) { onIntent(Intent.StartDailyChallenge) }
            .clearAndSetSemantics { contentDescription = description }
            .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Daily challenge · $detail",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.clip(PillShape)
                .background(if (done) Color.Transparent else colors.primary.copy(alpha = if (playable) 1f else 0.35f))
                .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            Text(
                if (done) "Done today" else "Play",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (done) colors.primary else colors.backgroundTop,
                maxLines = 1,
            )
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
private fun previewChallenge() = DailyChallengeGenerator().forDate(PreviewDay)

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
private fun NewPlayerPreview() = PreviewColumn(UiState(today = PreviewDay, todayChallenge = previewChallenge()))

@Preview
@Composable
private fun MidStreakAtRiskPreview() = PreviewColumn(
    UiState(
        today = PreviewDay,
        todayChallenge = previewChallenge(),
        progress = PlayerProgress(xp = 230, streak = StreakState(current = 5, best = 5, lastCompleted = LocalDate(2026, 11, 1))),
    ),
)

@Preview
@Composable
private fun ChallengeDonePreview() = PreviewColumn(
    UiState(
        today = PreviewDay,
        todayChallenge = previewChallenge(),
        progress = PlayerProgress(xp = 340, streak = StreakState(current = 6, best = 6, lastCompleted = PreviewDay)),
    ),
)

@Preview
@Composable
private fun LoadingBannerPreview() = PreviewColumn(UiState(today = PreviewDay, todayChallenge = null))
