package com.example.tictactoe.ui.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoe.domain.focus.GoNoGoEngine
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

@Composable
fun GoNoGoDrillScreen(
    onComplete: (scorePercent: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { GoNoGoEngine(totalTrials = 25) }

    var currentIndex by remember { mutableStateOf(engine.currentIndex) }
    var isFinished by remember { mutableStateOf(engine.isFinished()) }
    var feedbackText by remember { mutableStateOf<String?>(null) }
    var trialStartTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Automatic trial progression timer (850ms per trial)
    LaunchedEffect(currentIndex, isFinished) {
        if (!isFinished) {
            trialStartTime = System.currentTimeMillis()
            delay(850L)
            // Time out: user did not tap
            val correct = engine.recordTimeout()
            feedbackText = if (!correct) "Missed target!" else null
            currentIndex = engine.currentIndex
            isFinished = engine.isFinished()
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
        if (!isFinished) {
            val trial = engine.currentTrial()
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
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
                            onClick = onBack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Back", color = colors.muted, fontSize = 12.sp)
                        }
                        Text(
                            text = "GO / NO-GO REFLEX DRILL",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { currentIndex.toFloat() / 25f },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = colors.primary,
                        trackColor = colors.panel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trial $currentIndex of 25",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }

                // Interactive stimulus tap area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clickable {
                            val reactionTime = System.currentTimeMillis() - trialStartTime
                            val correct = engine.recordTap(reactionTime)
                            feedbackText = if (correct) "✓ Rapid Hit (${reactionTime}ms)" else "⚠ False Alarm! (No-Go tap)"
                            currentIndex = engine.currentIndex
                            isFinished = engine.isFinished()
                        }
                        .border(1.dp, colors.panelBorder, RoundedCornerShape(20.dp))
                        .background(colors.panel, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (trial.isGo) {
                            // Green Circle for GO
                            Canvas(modifier = Modifier.size(130.dp)) {
                                drawCircle(color = Color(0xFF43A047))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "TAP FAST!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF81C784)
                            )
                        } else {
                            // Red Diamond for NO-GO
                            Canvas(modifier = Modifier.size(130.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height / 2f)
                                    lineTo(size.width / 2f, size.height)
                                    lineTo(0f, size.height / 2f)
                                    close()
                                }
                                drawPath(path, color = Color(0xFFE53935))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "FREEZE! DO NOT TAP",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE53935)
                            )
                        }
                    }
                }

                // Feedback and instructions
                Column(
                    modifier = Modifier.padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = feedbackText ?: "Tap anywhere on Green • Freeze on Red",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (feedbackText?.contains("False") == true) Color(0xFFFF8A80) else colors.muted,
                        fontWeight = FontWeight.SemiBold
                    )
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
                        text = "REFLEX INHIBITION COMPLETE",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${engine.accuracyPercent()}%",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Motor Gating Accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Average Reaction Speed: ${engine.averageReactionTimeMillis()}ms\nImpulsive Taps on Red: ${engine.commissionErrors}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onComplete(engine.accuracyPercent()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Claim Rewards & Finish",
                            color = colors.backgroundTop,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

