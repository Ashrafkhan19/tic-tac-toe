package com.example.tictactoe.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Mode
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.presentation.UiState
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun Scoreboard(ui: UiState, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val vsCpu = ui.mode == Mode.VsComputer
    val outcome = ui.game.outcome
    fun active(mark: Mark) =
        (outcome == Outcome.InProgress && ui.game.turn == mark) || (outcome is Outcome.Won && outcome.winner == mark)

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ScoreCard(if (vsCpu) "You" else "Player X", ui.xWins, colors.x, active(Mark.X), Modifier.weight(1f))
        ScoreCard("Draws", ui.draws, colors.muted, outcome == Outcome.Draw, Modifier.weight(1f))
        ScoreCard(if (vsCpu) "Computer" else "Player O", ui.oWins, colors.o, active(Mark.O), Modifier.weight(1f))
    }
}

@Composable
private fun ScoreCard(label: String, value: Int, accent: Color, active: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalGameColors.current
    val shape = RoundedCornerShape(18.dp)
    val border by animateColorAsState(if (active) accent else colors.cellBorder, label = "scoreBorder")
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "scoreScale",
    )
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (active) accent.copy(alpha = 0.14f) else colors.cell)
            .border(if (active) 2.dp else 1.dp, border, shape)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "scoreValue",
        ) { score ->
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
