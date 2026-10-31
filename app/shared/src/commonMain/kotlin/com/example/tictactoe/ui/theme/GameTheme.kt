package com.example.tictactoe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.tictactoe.domain.model.Mark

@Immutable
class GameColors(
    val isDark: Boolean,
    val primary: Color,
    val x: Color,
    val o: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val glowA: Color,
    val glowB: Color,
    val glowStrength: Float,
    val cell: Color,
    val cellBorder: Color,
    val panel: Color,
    val panelBorder: Color,
    val onSurface: Color,
    val muted: Color,
) {
    fun forMark(mark: Mark): Color = if (mark == Mark.X) x else o
}

private val AuroraColors = GameColors(
        isDark = true, primary = Color(0xFF2DD4BF),
        x = Color(0xFF2DD4BF), o = Color(0xFFB794FF),
        backgroundTop = Color(0xFF06141B), backgroundBottom = Color(0xFF0E1F33),
        glowA = Color(0xFF14B8A6), glowB = Color(0xFF8B5CF6), glowStrength = 0.26f,
        cell = Color(0x1AFFFFFF), cellBorder = Color(0x26FFFFFF),
        panel = Color(0x12FFFFFF), panelBorder = Color(0x1FFFFFFF),
        onSurface = Color(0xFFE8F4F8), muted = Color(0xFF8CA7B5),
)

val LocalGameColors = staticCompositionLocalOf<GameColors> { error("TicTacToeTheme missing") }

@Composable
fun TicTacToeTheme(content: @Composable () -> Unit) {
    val colors = AuroraColors
    val onPrimary = if (colors.isDark) colors.backgroundTop else Color.White
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.primary, onPrimary = onPrimary,
            secondaryContainer = colors.primary.copy(alpha = 0.22f), onSecondaryContainer = colors.onSurface,
            surface = colors.backgroundTop, onSurface = colors.onSurface, outline = colors.muted.copy(alpha = 0.55f),
        )
    } else {
        lightColorScheme(
            primary = colors.primary, onPrimary = onPrimary,
            secondaryContainer = colors.primary.copy(alpha = 0.12f), onSecondaryContainer = colors.onSurface,
            surface = colors.backgroundTop, onSurface = colors.onSurface, outline = colors.muted.copy(alpha = 0.55f),
        )
    }
    CompositionLocalProvider(LocalGameColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
