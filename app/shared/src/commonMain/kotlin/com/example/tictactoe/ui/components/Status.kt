package com.example.tictactoe.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.presentation.UiState
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun StatusBanner(ui: UiState, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val vsCpu = ui.mode == Mode.VsComputer
    val (text, tint) = when (val outcome = ui.game.outcome) {
        Outcome.Draw -> "It's a draw!" to colors.muted
        is Outcome.Won -> when {
            vsCpu && outcome.winner == Mark.X -> "You win! 🎉"
            vsCpu -> "Computer wins"
            else -> "Player ${outcome.winner} wins! 🎉"
        } to colors.forMark(outcome.winner)
        Outcome.InProgress -> when {
            vsCpu && ui.game.turn == Mark.X -> "Your turn"
            vsCpu -> "Computer is thinking"
            else -> "Player ${ui.game.turn}'s turn"
        } to colors.forMark(ui.game.turn)
    }
    val animatedTint by animateColorAsState(tint, label = "statusTint")
    val shape = CircleShape

    Row(
        modifier
            .clip(shape)
            .background(animatedTint.copy(alpha = 0.14f))
            .border(1.dp, animatedTint.copy(alpha = 0.5f), shape)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
            label = "statusText",
        ) { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTint,
            )
        }
        if (ui.isComputerTurn) {
            Spacer(Modifier.width(8.dp))
            ThinkingDots(animatedTint)
        }
    }
}

@Composable
private fun ThinkingDots(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    val lifts = List(3) { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0f at 0
                    1f at 200
                    0f at 400
                    0f at 900
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(i * 140),
            ),
            label = "dot$i",
        )
    }
    Canvas(modifier.size(width = 28.dp, height = 16.dp)) {
        val radius = 2.5.dp.toPx()
        val lift = 4.dp.toPx()
        lifts.forEachIndexed { i, state ->
            drawCircle(color, radius, Offset(radius + i * (radius * 2 + 3.dp.toPx()), size.height / 2 - lift * state.value))
        }
    }
}
