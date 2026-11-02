package com.example.tictactoe.ui.focus.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.tictactoe.domain.focus.ArrowDirection
import com.example.tictactoe.domain.focus.FlankerEngine
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun FlankerDrillScreen(
    onComplete: (scorePercent: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { FlankerEngine(totalTrials = 30) }

    var currentTrial by remember { mutableStateOf(engine.currentTrial()) }
    var completedCount by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    var trialStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var feedbackColor by remember { mutableStateOf<Color?>(null) }

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
                            onClick = onBack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Back", color = colors.muted, fontSize = 12.sp)
                        }
                        Text(
                            text = "ERIKSEN FLANKER TASK",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / 30f },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = colors.primary,
                        trackColor = colors.panel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trial ${completedCount + 1} of 30",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }

                // Central Arrow Stimulus Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(
                            2.dp,
                            feedbackColor ?: colors.panelBorder,
                            RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = colors.panel),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FOCUS ON THE CENTER ARROW ONLY",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentTrial.arrows,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 8.sp,
                                color = colors.onSurface
                            )
                        }
                    }
                }

                // Direction Action Buttons (Left / Right)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            val reaction = System.currentTimeMillis() - trialStartTime
                            val correct = engine.submitAnswer(ArrowDirection.LEFT, reaction)
                            feedbackColor = if (correct) Color(0xFF43A047) else Color(0xFFE53935)
                            completedCount = engine.completedCount
                            isFinished = engine.isFinished()
                            if (!isFinished) {
                                currentTrial = engine.currentTrial()
                                trialStartTime = System.currentTimeMillis()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.panel),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.primary),
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) {
                        Text("◀ LEFT", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }

                    Button(
                        onClick = {
                            val reaction = System.currentTimeMillis() - trialStartTime
                            val correct = engine.submitAnswer(ArrowDirection.RIGHT, reaction)
                            feedbackColor = if (correct) Color(0xFF43A047) else Color(0xFFE53935)
                            completedCount = engine.completedCount
                            isFinished = engine.isFinished()
                            if (!isFinished) {
                                currentTrial = engine.currentTrial()
                                trialStartTime = System.currentTimeMillis()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.panel),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.primary),
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) {
                        Text("RIGHT ▶", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
        } else {
            // Results Card
            val conflictCost = engine.conflictCostMs()
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
                        text = "FLANKER SELECTIVITY COMPLETE",
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
                        text = "Target Isolation Accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Flanker Conflict Delay: +${conflictCost}ms\n" + if (conflictCost <= 50) {
                            "Top 10% Visual Gating! Your anterior cingulate cortex effortlessly filters out visual noise."
                        } else {
                            "Good workout! Repeated gating training stops peripheral phone pop-ups from hijacking attention."
                        },
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

