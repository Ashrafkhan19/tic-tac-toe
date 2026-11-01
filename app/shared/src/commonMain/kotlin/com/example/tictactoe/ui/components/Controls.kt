package com.example.tictactoe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.presentation.UiState
import com.example.tictactoe.ui.theme.LocalGameColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelector(ui: UiState, onIntent: (Intent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Segmented(
            options = listOf("vs Computer" to Mode.VsComputer, "2 Players" to Mode.TwoPlayer),
            selected = ui.mode,
            onSelect = { onIntent(Intent.ModeSelected(it)) },
        )
        AnimatedVisibility(
            visible = ui.mode == Mode.VsComputer,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Segmented(
                options = listOf("Easy" to Difficulty.Easy, "Medium" to Difficulty.Medium, "Unbeatable" to Difficulty.Unbeatable),
                selected = ui.difficulty,
                onSelect = { onIntent(Intent.DifficultySelected(it)) },
            )
        }
    }
}

/** Above this font scale three labels no longer fit side by side, so the choices stack as radio rows. */
internal const val LargeFontScale = 1.3f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    if (LocalDensity.current.fontScale > LargeFontScale) {
        StackedChoices(options, selected, onSelect, modifier)
        return
    }
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, value) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                // Two lines before ever clipping a word.
                Text(label, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun <T> StackedChoices(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    Column(modifier.fillMaxWidth().selectableGroup()) {
        options.forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth()
                    .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Text(label, color = colors.onSurface, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ActionButtons(ui: UiState, onIntent: (Intent) -> Unit, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "nextPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulse",
    )
    val roundOver = ui.game.isOver
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { onIntent(Intent.NewRound) },
            modifier = Modifier.weight(1f).graphicsLayer {
                // Nudge the player toward the next round once this one is finished.
                val s = if (roundOver) pulse else 1f
                scaleX = s
                scaleY = s
            },
        ) { Text(if (ui.inChallenge) "Retry" else "New round", textAlign = TextAlign.Center) }
        // In a challenge Retry already restarts the puzzle and there is no session score to reset.
        if (!ui.inChallenge) {
            OutlinedButton(onClick = { onIntent(Intent.ResetScores) }, modifier = Modifier.weight(1f)) {
                Text("Reset scores", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun VariantBadge(
    variant: GameVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalGameColors.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.panel)
            .border(
                1.dp,
                if (enabled) colors.primary.copy(alpha = 0.5f) else colors.panelBorder,
                shape,
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = "Change variant",
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = variant.displayName,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) colors.primary else colors.muted,
        )
        if (enabled) {
            Text(
                text = "▾",
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
            )
        }
    }
}
