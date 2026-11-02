package com.example.tictactoe.ui.focus.restoration

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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

private data class SomaticZone(
    val title: String,
    val icon: String,
    val tensionPrompt: String,
    val releasePrompt: String
)

@Composable
fun SomaticResetScreen(
    onFinished: (xpAwarded: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGameColors.current

    val zones = remember {
        listOf(
            SomaticZone(
                title = "Hands & Forearms",
                icon = "✊",
                tensionPrompt = "Inhale & clench your fists tightly into balls...",
                releasePrompt = "Exhale & unclench, feeling warmth flood your fingers..."
            ),
            SomaticZone(
                title = "Shoulders & Neck",
                icon = "🧘",
                tensionPrompt = "Inhale & shrug your shoulders high up to your ears...",
                releasePrompt = "Exhale & let your shoulders drop completely limp..."
            ),
            SomaticZone(
                title = "Face & Jaw",
                icon = "😌",
                tensionPrompt = "Inhale & scrunch your eyes and clench your jaw...",
                releasePrompt = "Exhale & let your jaw hang loose, smoothing your forehead..."
            ),
            SomaticZone(
                title = "Diaphragm & Torso",
                icon = "🌬️",
                tensionPrompt = "Inhale deep & gently brace your stomach core...",
                releasePrompt = "Long exhale through mouth, melting all physical tension..."
            )
        )
    }

    var currentZoneIndex by remember { mutableStateOf(0) }
    var secondsInZone by remember { mutableStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }

    // Progress through 4 zones, 30s each (total 120s)
    LaunchedEffect(currentZoneIndex, isComplete) {
        if (!isComplete) {
            while (secondsInZone < 30) {
                delay(1000L)
                secondsInZone++
            }
            if (currentZoneIndex < zones.lastIndex) {
                currentZoneIndex++
                secondsInZone = 0
            } else {
                isComplete = true
            }
        }
    }

    // 10s tension/release breathing cycle: 4s inhale/tension, 6s exhale/release
    val infiniteTransition = rememberInfiniteTransition(label = "somaticCycle")
    val cycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycleProgress"
    )

    val isTensionPhase = cycleProgress < 4f
    val circleScale = if (isTensionPhase) {
        1.0f + 0.9f * (cycleProgress / 4f)
    } else {
        1.9f - 0.9f * ((cycleProgress - 4f) / 6f)
    }

    val activeZone = zones[currentZoneIndex]

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
        if (!isComplete) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Exit", color = colors.muted, fontSize = 12.sp)
                        }
                        Text(
                            text = "SOMATIC BODY RESET",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    val overallProgress = (currentZoneIndex * 30 + secondsInZone).toFloat() / 120f
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = colors.primary,
                        trackColor = colors.panel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Zone ${currentZoneIndex + 1} of 4: ${activeZone.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }

                // Breathing Canvas with Zone Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
                ) {
                    val primaryColor = colors.primary
                    val glowColor = if (isTensionPhase) Color(0xFFFFB74D) else colors.glowA
                    Canvas(modifier = Modifier.size(240.dp)) {
                        val radius = (size.minDimension / 4.4f) * circleScale
                        drawCircle(
                            color = glowColor.copy(alpha = 0.22f),
                            radius = radius * 1.25f
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.45f),
                            radius = radius
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = activeZone.icon, fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isTensionPhase) "TENSE ON INHALE" else "RELEASE ON EXHALE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isTensionPhase) Color(0xFFFFCC80) else colors.onSurface
                        )
                    }
                }

                // Active instructions card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.panel),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isTensionPhase) activeZone.tensionPrompt else activeZone.releasePrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Results Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SOMATIC RESET COMPLETE",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🌿",
                        fontSize = 54.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Physical Cortisol Released",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Screen stress and chronic neck tension have been purged. Your parasympathetic nervous system is restored (+${FocusXpRules.xpForUrgeSurfed()} XP).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onFinished(FocusXpRules.xpForUrgeSurfed()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Claim Focus XP",
                            color = colors.backgroundTop,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

