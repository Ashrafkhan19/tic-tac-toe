package com.example.tictactoe.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.GridBoard
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

object BoardLayoutCalculator {
    fun calculateCellSize(containerSize: Float, gap: Float, gridSize: Int): Float =
        (containerSize - gap * (gridSize - 1)) / gridSize

    fun cellCenter(index: Int, gridSize: Int, cellSize: Float, gap: Float): Pair<Float, Float> {
        val col = index % gridSize
        val row = index / gridSize
        val x = col * (cellSize + gap) + cellSize / 2f
        val y = row * (cellSize + gap) + cellSize / 2f
        return x to y
    }
}

@Composable
fun MarkSymbol(
    mark: Mark,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    MarkView(mark = mark, modifier = modifier, animated = animated)
}

@Composable
fun GridWinLine(
    won: Outcome.Won,
    size: Int,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    val color = LocalGameColors.current.forMark(won.winner)
    val progress = remember(won.line) { Animatable(0f) }
    LaunchedEffect(won.line) {
        delay(280)
        progress.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        if (progress.value <= 0f) return@Canvas
        val gapPx = gap.toPx()
        val cellSize = BoardLayoutCalculator.calculateCellSize(this.size.width, gapPx, size)
        val (ax, ay) = BoardLayoutCalculator.cellCenter(won.line.first(), size, cellSize, gapPx)
        val (bx, by) = BoardLayoutCalculator.cellCenter(won.line.last(), size, cellSize, gapPx)
        val a = Offset(ax, ay)
        val b = Offset(bx, by)
        val distance = (b - a).getDistance()
        if (distance <= 0f) return@Canvas
        val direction = (b - a) / distance
        val start = a - direction * (cellSize * 0.34f)
        val end = b + direction * (cellSize * 0.34f)
        val tip = start + (end - start) * progress.value
        drawLine(color.copy(alpha = 0.28f), start, tip, cellSize * 0.2f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.9f), start, tip, cellSize * 0.05f, StrokeCap.Round)
        drawLine(color, start, tip, cellSize * 0.09f, StrokeCap.Round)
    }
}

@Composable
fun AdaptiveGridBoard(
    board: GridBoard,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showWinLine: Boolean = true,
    cellGap: Dp? = null,
    cornerRadius: Dp? = null,
) {
    val colors = LocalGameColors.current
    val size = board.size
    val effectiveCellGap = cellGap ?: when (size) {
        3 -> 8.dp
        4 -> 6.dp
        else -> 4.dp
    }
    val effectiveCornerRadius = cornerRadius ?: when (size) {
        3 -> 12.dp
        4 -> 8.dp
        else -> 6.dp
    }
    val cellShape = RoundedCornerShape(effectiveCornerRadius)
    val won = board.outcome() as? Outcome.Won
    val isGameOver = board.outcome() !is Outcome.InProgress

    Box(modifier = modifier.aspectRatio(1f).fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(effectiveCellGap),
        ) {
            for (r in 0 until size) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(effectiveCellGap),
                ) {
                    for (c in 0 until size) {
                        val index = r * size + c
                        val mark = board.cells[index]
                        val isWinningCell = won != null && index in won.line
                        val cellClickable = enabled && mark == null && !isGameOver

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(cellShape)
                                .background(
                                    if (isWinningCell) colors.forMark(won.winner).copy(alpha = 0.2f)
                                    else colors.cell
                                )
                                .border(
                                    width = if (isWinningCell) 2.dp else 1.dp,
                                    color = when {
                                        isWinningCell -> colors.forMark(won.winner)
                                        mark != null -> colors.cellBorder
                                        enabled -> colors.cellBorder.copy(alpha = 0.5f)
                                        else -> colors.cellBorder.copy(alpha = 0.2f)
                                    },
                                    shape = cellShape,
                                )
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "Cell ${index + 1}: ${mark?.name ?: "empty"}"
                                }
                                .clickable(
                                    enabled = cellClickable,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button,
                                    onClick = { onCellClick(index) },
                                ),
                        ) {
                            if (mark != null) {
                                MarkSymbol(mark = mark, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
        if (showWinLine && won != null) {
            GridWinLine(won = won, size = size, gap = effectiveCellGap, modifier = Modifier.matchParentSize())
        }
    }
}
