package com.example.tictactoe.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.tictactoe.domain.focus.CognitiveBenchmark
import com.example.tictactoe.domain.focus.TrailMakingEngine
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

@Composable
fun TrailMakingScreen(
    onComplete: (completionSeconds: Double) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { TrailMakingEngine(targetPairsCount = 6) } // 1..6 and A..F (12 targets)

    var currentStep by remember { mutableStateOf(engine.currentStep) }
    var penaltyCount by remember { mutableStateOf(engine.penaltyCount) }
    var isFinished by remember { mutableStateOf(engine.isFinished) }
    var elapsedSeconds by remember { mutableStateOf(0.0) }
    val completedLabels = remember { mutableStateListOf<String>() }

    LaunchedEffect(isFinished) {
        while (!isFinished) {
            delay(100L)
            elapsedSeconds += 0.1
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
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isFinished) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Back", color = colors.muted, fontSize = 12.sp)
                    }
                    Text(
                        text = "TRAIL MAKING (1 ➔ A ➔ 2 ➔ B)",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Stats Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next Target: ${engine.currentExpectedLabel() ?: ""}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Time: ${((elapsedSeconds * 10).toInt()) / 10.0}s | Penalties: $penaltyCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }

                // Interactive Scattered Target Field
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, colors.panelBorder, RoundedCornerShape(16.dp))
                        .background(colors.panel, RoundedCornerShape(16.dp))
                ) {
                    val fieldWidth = maxWidth
                    val fieldHeight = maxHeight

                    engine.allTargets.forEach { target ->
                        val isNext = target.label == engine.currentExpectedLabel()
                        val isDone = completedLabels.contains(target.label)

                        val xPos = fieldWidth * target.xRatio
                        val yPos = fieldHeight * target.yRatio

                        Box(
                            modifier = Modifier
                                .offset(x = xPos - 22.dp, y = yPos - 22.dp)
                                .size(44.dp)
                                .background(
                                    when {
                                        isDone -> colors.primary.copy(alpha = 0.2f)
                                        isNext -> colors.primary
                                        else -> colors.backgroundTop
                                    },
                                    CircleShape
                                )
                                .border(
                                    1.5.dp,
                                    if (isNext) colors.primary else colors.panelBorder,
                                    CircleShape
                                )
                                .clickable(enabled = !isDone) {
                                    val success = engine.tapTarget(target.label)
                                    if (success) {
                                        completedLabels.add(target.label)
                                        currentStep = engine.currentStep
                                        isFinished = engine.isFinished
                                    } else {
                                        penaltyCount = engine.penaltyCount
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = target.label,
                                color = if (isNext) colors.backgroundTop else colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Tap numbers & letters in alternating order as fast as possible",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
        } else {
            // Results Card
            val totalTime = elapsedSeconds + penaltyCount * 1.5
            val percentile = CognitiveBenchmark.flexibilityPercentile(totalTime)
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
                        text = "TASK SWITCHING COMPLETE",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${((totalTime * 10).toInt()) / 10.0}s",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Top ${100 - percentile}% of players worldwide",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Penalties incurred: $penaltyCount • Executive cognitive shifting capacity verified.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onComplete(totalTime) },
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

