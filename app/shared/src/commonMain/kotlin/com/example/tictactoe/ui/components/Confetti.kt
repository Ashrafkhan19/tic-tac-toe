package com.example.tictactoe.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private class Particle(
    val x: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val rotation: Float,
    val spin: Float,
    val color: Color,
)

private const val DURATION_MS = 2800
private const val GRAVITY = 1.5f
private const val COUNT = 90

/**
 * One-shot confetti burst. A new non-negative [trigger] value fires it; a negative value keeps it idle.
 * Particle positions are closed-form in time, so there is no per-frame state besides the clock.
 */
@Composable
fun Confetti(trigger: Int, palette: List<Color>, modifier: Modifier = Modifier) {
    val clock = remember { Animatable(1f) }
    val particles = remember(trigger, palette) {
        val random = Random(trigger)
        List(COUNT) {
            Particle(
                x = random.nextFloat(),
                vx = random.nextFloat() * 0.5f - 0.25f,
                vy = random.nextFloat() * 0.5f - 0.1f,
                size = 6f + random.nextFloat() * 8f,
                rotation = random.nextFloat() * 360f,
                spin = random.nextFloat() * 720f - 360f,
                color = palette[random.nextInt(palette.size)],
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger >= 0) {
            clock.snapTo(0f)
            clock.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        }
    }
    Canvas(modifier) {
        val t = clock.value
        if (t <= 0f || t >= 1f) return@Canvas
        val fade = ((1f - t) / 0.25f).coerceIn(0f, 1f)
        particles.forEach { p ->
            val x = (p.x + p.vx * t) * size.width
            val y = (-0.05f + p.vy * t + GRAVITY * t * t) * size.height
            if (y > size.height + 20f) return@forEach
            val s = p.size * density
            rotate(p.rotation + p.spin * t, pivot = Offset(x, y)) {
                drawRect(p.color.copy(alpha = fade), Offset(x - s / 2, y - s * 0.3f), Size(s, s * 0.6f))
            }
        }
    }
}
