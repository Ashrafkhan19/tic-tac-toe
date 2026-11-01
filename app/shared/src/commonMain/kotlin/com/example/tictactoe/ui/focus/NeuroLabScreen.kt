package com.example.tictactoe.ui.focus

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoe.domain.focus.StroopColor
import com.example.tictactoe.domain.focus.StroopEngine
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NeuroLabScreen(
    onDrillComplete: (scorePercent: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { StroopEngine(totalTrials = 30) }
    var currentTrial by remember { mutableStateOf(engine.currentTrial()) }
    var completedCount by remember { mutableStateOf(engine.completedTrialsCount()) }
    var feedbackColor by remember { mutableStateOf<Color?>(null) }
    var isFinished by remember { mutableStateOf(engine.isFinished()) }
    val scope = rememberCoroutineScope()

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
                // Top Header & Progress
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
                            text = "NEURO LAB: STROOP DRILL",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / 30f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = colors.primary,
                        trackColor = colors.panel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Trial $completedCount of 30",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }

                // Center Stimulus Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .then(
                            if (feedbackColor != null) {
                                Modifier.border(2.dp, feedbackColor!!, RoundedCornerShape(16.dp))
                            } else {
                                Modifier.border(1.dp, colors.panelBorder, RoundedCornerShape(16.dp))
                            }
                        ),
                    colors = CardDefaults.cardColors(containerColor = colors.panel),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TAP THE FONT COLOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentTrial.word.displayName,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(currentTrial.fontColor.hexColor)
                            )
                        }
                    }
                }

                // 4 Color Choice Buttons
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val allColors = StroopColor.entries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorChoiceButton(
                            colorOption = allColors[0],
                            modifier = Modifier.weight(1f)
                        ) {
                            handleColorChoice(allColors[0], engine, scope) { correct, finished ->
                                feedbackColor = if (correct) Color(0xFF43A047) else Color(0xFFE53935)
                                currentTrial = engine.currentTrial()
                                completedCount = engine.completedTrialsCount()
                                isFinished = finished
                            }
                        }
                        ColorChoiceButton(
                            colorOption = allColors[1],
                            modifier = Modifier.weight(1f)
                        ) {
                            handleColorChoice(allColors[1], engine, scope) { correct, finished ->
                                feedbackColor = if (correct) Color(0xFF43A047) else Color(0xFFE53935)
                                currentTrial = engine.currentTrial()
                                completedCount = engine.completedTrialsCount()
                                isFinished = finished
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorChoiceButton(
                            colorOption = allColors[2],
                            modifier = Modifier.weight(1f)
                        ) {
                            handleColorChoice(allColors[2], engine, scope) { correct, finished ->
                                feedbackColor = if (correct) Color(0xFF43A047) else Color(0xFFE53935)
                                currentTrial = engine.currentTrial()
                                completedCount = engine.completedTrialsCount()
                                isFinished = finished
                            }
                        }
                        ColorChoiceButton(
                            colorOption = allColors[3],
                            modifier = Modifier.weight(1f)
                        ) {
                            handleColorChoice(allColors[3], engine, scope) { correct, finished ->
                                feedbackColor = if (correct) Color(0xFF43A047) else Color(0xFFE53935)
                                currentTrial = engine.currentTrial()
                                completedCount = engine.completedTrialsCount()
                                isFinished = finished
                            }
                        }
                    }
                }
            }
        } else {
            // Summary Card
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
                        text = "DRILL COMPLETE",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${engine.accuracyPercent()}%",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Inhibitory Control Accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (engine.accuracyPercent() >= 80) {
                            "Excellent executive suppression! Your prefrontal cortex is actively overriding automatic impulses."
                        } else {
                            "Good workout! Repeated practice restores cognitive stamina and decreases distraction vulnerability."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onDrillComplete(engine.accuracyPercent()) },
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

@Composable
private fun ColorChoiceButton(
    colorOption: StroopColor,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(colorOption.hexColor)),
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = colorOption.displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

private fun handleColorChoice(
    color: StroopColor,
    engine: StroopEngine,
    scope: kotlinx.coroutines.CoroutineScope,
    onUpdated: (correct: Boolean, isFinished: Boolean) -> Unit
) {
    val correct = engine.submitAnswer(color, timestampMillis = System.currentTimeMillis())
    val finished = engine.isFinished()
    onUpdated(correct, finished)
}
