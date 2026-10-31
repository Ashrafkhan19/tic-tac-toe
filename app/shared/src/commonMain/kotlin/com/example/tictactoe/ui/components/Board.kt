package com.example.tictactoe.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.Board
import com.example.tictactoe.domain.model.GameState
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

private val CellGap = 10.dp
private val CellShape = RoundedCornerShape(20.dp)
private const val SIZE = 3

@Composable
fun Board(
    game: GameState,
    enabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val won = game.outcome as? Outcome.Won
    Box(modifier.aspectRatio(1f)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(CellGap)) {
            repeat(SIZE) { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    repeat(SIZE) { col ->
                        val index = row * SIZE + col
                        val mark = game.board.cells[index]
                        Cell(
                            index = index,
                            mark = mark,
                            previewMark = game.turn,
                            enabled = enabled && mark == null && !game.isOver,
                            winner = won?.takeIf { index in it.line }?.winner,
                            onClick = { onCellClick(index) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }
        if (won != null) WinLine(won, Modifier.matchParentSize())
    }
}

@Composable
private fun Cell(
    index: Int,
    mark: Mark?,
    previewMark: Mark,
    enabled: Boolean,
    winner: Mark?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGameColors.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            enabled && pressed -> 0.92f
            enabled && hovered -> 1.04f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "cellScale",
    )
    val pulse = if (winner != null) {
        rememberInfiniteTransition(label = "winPulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulse",
        )
    } else {
        null
    }
    val border = winner?.let { colors.forMark(it) } ?: if (enabled && hovered) colors.onSurface.copy(alpha = 0.4f) else colors.cellBorder

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CellShape)
            .drawBehind {
                drawRect(colors.cell)
                if (winner != null) {
                    drawRect(colors.forMark(winner).copy(alpha = 0.16f + 0.14f * (pulse?.value ?: 0f)))
                }
            }
            .border(if (winner != null) 2.dp else 1.dp, border, CellShape)
            .semantics(mergeDescendants = true) { contentDescription = "Cell ${index + 1}: ${mark?.name ?: "empty"}" }
            .hoverable(interaction, enabled)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        if (mark != null) {
            MarkView(mark, Modifier.fillMaxSize())
        } else if (enabled && hovered) {
            MarkView(previewMark, Modifier.fillMaxSize().alpha(0.28f), animated = false)
        }
    }
}

/** Sweeps a glowing line through the winning cells shortly after the last mark lands. */
@Composable
private fun WinLine(won: Outcome.Won, modifier: Modifier = Modifier) {
    val color = LocalGameColors.current.forMark(won.winner)
    val progress = remember(won.line) { Animatable(0f) }
    LaunchedEffect(won.line) {
        delay(280)
        progress.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        if (progress.value <= 0f) return@Canvas
        val gap = CellGap.toPx()
        val cell = (size.width - gap * (SIZE - 1)) / SIZE
        fun center(i: Int) = Offset((i % SIZE) * (cell + gap) + cell / 2, (i / SIZE) * (cell + gap) + cell / 2)
        val a = center(won.line.first())
        val b = center(won.line.last())
        val direction = (b - a) / (b - a).getDistance()
        val start = a - direction * (cell * 0.34f)
        val end = b + direction * (cell * 0.34f)
        val tip = start + (end - start) * progress.value
        drawLine(color.copy(alpha = 0.28f), start, tip, cell * 0.2f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.9f), start, tip, cell * 0.05f, StrokeCap.Round)
        drawLine(color, start, tip, cell * 0.09f, StrokeCap.Round)
    }
}
