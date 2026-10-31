package com.example.tictactoe.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.launch

/** An X or O that draws itself in and pops with a spring. Pass [animated] = false for static previews. */
@Composable
fun MarkView(mark: Mark, modifier: Modifier = Modifier, animated: Boolean = true) {
    val color = LocalGameColors.current.forMark(mark)
    val progress = remember { Animatable(if (animated) 0f else 1f) }
    val pop = remember { Animatable(if (animated) 0.55f else 1f) }
    LaunchedEffect(Unit) {
        if (animated) {
            launch { progress.animateTo(1f, tween(340, easing = FastOutSlowInEasing)) }
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
    }
    Canvas(
        modifier.graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
        },
    ) {
        inset(size.minDimension * 0.22f) { drawMark(mark, progress.value, color) }
    }
}

private fun DrawScope.drawMark(mark: Mark, progress: Float, color: Color) {
    val core = size.minDimension * 0.15f
    val glow = core * 2.4f
    when (mark) {
        Mark.X -> {
            val first = (progress * 2f).coerceIn(0f, 1f)
            val second = (progress * 2f - 1f).coerceIn(0f, 1f)
            val tl = Offset.Zero
            val br = Offset(size.width, size.height)
            val tr = Offset(size.width, 0f)
            val bl = Offset(0f, size.height)
            for ((width, alpha) in listOf(glow to 0.18f, core to 1f)) {
                val c = color.copy(alpha = alpha)
                if (first > 0f) drawLine(c, tl, tl + (br - tl) * first, width, StrokeCap.Round)
                if (second > 0f) drawLine(c, tr, tr + (bl - tr) * second, width, StrokeCap.Round)
            }
        }
        Mark.O -> for ((width, alpha) in listOf(glow to 0.18f, core to 1f)) {
            drawArc(
                color = color.copy(alpha = alpha),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(core / 2, core / 2),
                size = Size(size.width - core, size.height - core),
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
        }
    }
}
