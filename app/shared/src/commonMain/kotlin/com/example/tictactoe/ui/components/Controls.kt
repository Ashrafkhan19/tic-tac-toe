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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.tictactoe.ui.theme.LocalGameColors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.presentation.UiState

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, value) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label, maxLines = 1) }
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
        ) { Text("New round") }
        OutlinedButton(onClick = { onIntent(Intent.ResetScores) }, modifier = Modifier.weight(1f)) { Text("Reset scores") }
    }
}
