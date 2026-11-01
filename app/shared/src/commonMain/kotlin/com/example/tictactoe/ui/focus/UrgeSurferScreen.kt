package com.example.tictactoe.ui.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoe.domain.focus.FocusXpRules
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

@Composable
fun UrgeSurferScreen(
    onFinished: (xpAwarded: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGameColors.current
    var secondsRemaining by remember { mutableStateOf(60) }
    var isComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
        isComplete = true
    }

    // Physiological sigh cycle: 6500ms
    val infiniteTransition = rememberInfiniteTransition(label = "breathCycle")
    val breathProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathProgress"
    )

    // Calculate breath phase and visual scale
    val (instructionText, circleScale) = remember(breathProgress) {
        when {
            breathProgress < 1.5f -> {
                val fraction = breathProgress / 1.5f
                "Inhale deeply through nose..." to (1.0f + 0.6f * fraction)
            }
            breathProgress < 2.5f -> {
                val fraction = (breathProgress - 1.5f) / 1.0f
                "Top-off sharp inhale..." to (1.6f + 0.4f * fraction)
            }
            breathProgress < 3.0f -> {
                "Hold..." to 2.0f
            }
            else -> {
                val fraction = (breathProgress - 3.0f) / 3.5f
                "Slow exhale through mouth..." to (2.0f - 1.0f * fraction)
            }
        }
    }

    val groundingReminder = remember(secondsRemaining) {
        when {
            secondsRemaining > 45 -> "The craving is just a wave of dopamine anticipation."
            secondsRemaining > 25 -> "It peaks within 60 seconds, then naturally fades."
            secondsRemaining > 5 -> "Your prefrontal cortex is regaining executive control."
            else -> "Urge surfed! You broke the automatic reflex."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "URGE SURFER",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isComplete) "Completed!" else "${secondsRemaining}s reset",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
            }

            // Central Animated Breathing Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                val primaryColor = colors.primary
                val glowColor = colors.glowA
                Canvas(modifier = Modifier.size(220.dp)) {
                    val radius = (size.minDimension / 4.4f) * circleScale
                    drawCircle(
                        color = glowColor.copy(alpha = 0.18f),
                        radius = radius * 1.25f
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.45f),
                        radius = radius
                    )
                }

                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Grounding prompt & Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = groundingReminder,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (isComplete) {
                    Button(
                        onClick = { onFinished(FocusXpRules.xpForUrgeSurfed()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Claim Victory (+${FocusXpRules.xpForUrgeSurfed()} XP)",
                            color = colors.backgroundTop,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exit Early", color = colors.muted)
                    }
                }
            }
        }
    }
}
