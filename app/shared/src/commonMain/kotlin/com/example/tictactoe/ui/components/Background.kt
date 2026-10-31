package com.example.tictactoe.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Gradient backdrop with two soft glows drifting slowly; the motion is read only in the draw phase. */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = LocalGameColors.current
    val transition = rememberInfiniteTransition(label = "background")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            .drawBehind {
                val angle = phase * 2f * PI.toFloat()
                val radius = size.maxDimension * 0.65f
                fun glow(color: Color, center: Offset) = drawCircle(
                    brush = Brush.radialGradient(listOf(color.copy(alpha = colors.glowStrength), Color.Transparent), center, radius),
                    radius = radius,
                    center = center,
                )
                glow(colors.glowA, Offset(size.width * (0.2f + 0.12f * cos(angle)), size.height * (0.18f + 0.08f * sin(angle))))
                glow(colors.glowB, Offset(size.width * (0.85f - 0.12f * sin(angle)), size.height * (0.85f - 0.08f * cos(angle))))
            },
        content = content,
    )
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalGameColors.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(colors.panel)
            .border(1.dp, colors.panelBorder, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
