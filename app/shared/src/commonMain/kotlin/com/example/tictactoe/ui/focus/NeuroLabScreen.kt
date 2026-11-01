package com.example.tictactoe.ui.focus

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
import androidx.compose.foundation.lazy.LazyColumn
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

enum class NeuroDrillType {
    MENU,
    STROOP,
    GO_NO_GO,
    N_BACK,
    CORSI_BLOCK,
    TRAIL_MAKING,
    SPEED_MATH,
    BACKWARD_SPAN,
    MATRIX_LOGIC
}

@Composable
fun NeuroLabScreen(
    onDrillComplete: (scorePercent: Int) -> Unit,
    onRecordReactionTime: (ms: Long) -> Unit = {},
    onRecordSpatialSpan: (span: Int) -> Unit = {},
    onRecordFlexibility: (seconds: Double) -> Unit = {},
    onRecordMathSpeed: (opsPerMinute: Int) -> Unit = {},
    onBack: () -> Unit
) {
    var activeDrill by remember { mutableStateOf(NeuroDrillType.MENU) }

    when (activeDrill) {
        NeuroDrillType.MENU -> {
            NeuroLabMenuView(
                onSelectDrill = { activeDrill = it },
                onBack = onBack
            )
        }
        NeuroDrillType.STROOP -> {
            StroopDrillView(
                onComplete = onDrillComplete,
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
        NeuroDrillType.GO_NO_GO -> {
            GoNoGoDrillScreen(
                onComplete = { score ->
                    onDrillComplete(score)
                },
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
        NeuroDrillType.N_BACK -> {
            NBackDrillScreen(
                onComplete = onDrillComplete,
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
        NeuroDrillType.CORSI_BLOCK -> {
            CorsiBlockScreen(
                onComplete = { span ->
                    onRecordSpatialSpan(span)
                    val scorePercent = (span * 15).coerceIn(40, 100)
                    onDrillComplete(scorePercent)
                },
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
        NeuroDrillType.TRAIL_MAKING -> {
            TrailMakingScreen(
                onComplete = { timeSeconds ->
                    onRecordFlexibility(timeSeconds)
                    val scorePercent = ((40.0 - timeSeconds) * 3).toInt().coerceIn(40, 100)
                    onDrillComplete(scorePercent)
                },
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
        NeuroDrillType.SPEED_MATH -> {
            SpeedMathScreen(
                onComplete = { opsPerMin ->
                    onRecordMathSpeed(opsPerMin)
                    val scorePercent = (opsPerMin * 5).coerceIn(40, 100)
                    onDrillComplete(scorePercent)
                },
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
        NeuroDrillType.BACKWARD_SPAN -> {
            BackwardSpanScreen(
                onComplete = { span ->
                    onRecordSpatialSpan(span)
                    val scorePercent = (span * 16).coerceIn(40, 100)
                    onDrillComplete(scorePercent)
                },
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
        NeuroDrillType.MATRIX_LOGIC -> {
            MatrixLogicScreen(
                onComplete = onDrillComplete,
                onBack = { activeDrill = NeuroDrillType.MENU }
            )
        }
    }
}

@Composable
private fun NeuroLabMenuView(
    onSelectDrill: (NeuroDrillType) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            )
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
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
                    text = "NEURO LAB DRILLS",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = "Clinical cognitive exercises designed to restore prefrontal control, expand memory, and halt impulsive reflexes.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    DrillMenuCard(
                        icon = "🔴",
                        title = "Stroop Interference Drill",
                        desc = "Inhibit automatic reading impulses by naming font colors under cognitive interference.",
                        onClick = { onSelectDrill(NeuroDrillType.STROOP) }
                    )
                }
                item {
                    DrillMenuCard(
                        icon = "⚡",
                        title = "Go / No-Go Motor Reflex",
                        desc = "Train motor inhibition. Tap rapidly on green targets, freeze completely on red diamonds.",
                        onClick = { onSelectDrill(NeuroDrillType.GO_NO_GO) }
                    )
                }
                item {
                    DrillMenuCard(
                        icon = "🔷",
                        title = "Visual 2-Back Stream",
                        desc = "The gold standard for fluid intelligence. Detect geometric matches shown 2 steps prior.",
                        onClick = { onSelectDrill(NeuroDrillType.N_BACK) }
                    )
                }
                item {
                    DrillMenuCard(
                        icon = "🟦",
                        title = "Corsi Block Spatial Span",
                        desc = "Recall and reproduce expanding sequences of spatial coordinates to strengthen hippocampal memory.",
                        onClick = { onSelectDrill(NeuroDrillType.CORSI_BLOCK) }
                    )
                }
                item {
                    DrillMenuCard(
                        icon = "🔄",
                        title = "Trail Making (1 ➔ A ➔ 2 ➔ B)",
                        desc = "Executive cognitive shifting. Rapidly alternate between numbers and letters under pressure.",
                        onClick = { onSelectDrill(NeuroDrillType.TRAIL_MAKING) }
                    )
                }
                item {
                    DrillMenuCard(
                        icon = "🧮",
                        title = "Speed Arithmetic Flow",
                        desc = "60-second mental math sprint to boost calculation speed and working memory bandwidth.",
                        onClick = { onSelectDrill(NeuroDrillType.SPEED_MATH) }
                    )
                }
                item {
                    DrillMenuCard(
                        icon = "⏪",
                        title = "Backward Memory Span",
                        desc = "Recall sequences in reverse order. Directly exercises active working memory manipulation.",
                        onClick = { onSelectDrill(NeuroDrillType.BACKWARD_SPAN) }
                    )
                }
                item {
                    DrillMenuCard(
                        icon = "🧩",
                        title = "Visual Matrix Reasoning",
                        desc = "Raven's matrices abstract puzzle solving to test and strengthen non-verbal fluid logic.",
                        onClick = { onSelectDrill(NeuroDrillType.MATRIX_LOGIC) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrillMenuCard(
    icon: String,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, colors.panelBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.panel),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = icon, fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StroopDrillView(
    onComplete: (scorePercent: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { StroopEngine(totalTrials = 30) }
    var currentTrial by remember { mutableStateOf(engine.currentTrial()) }
    var completedCount by remember { mutableStateOf(engine.completedTrialsCount()) }
    var feedbackColor by remember { mutableStateOf<Color?>(null) }
    var isFinished by remember { mutableStateOf(engine.isFinished()) }

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
                            text = "STROOP INHIBITORY DRILL",
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
                            handleStroopChoice(allColors[0], engine) { correct, finished ->
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
                            handleStroopChoice(allColors[1], engine) { correct, finished ->
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
                            handleStroopChoice(allColors[2], engine) { correct, finished ->
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
                            handleStroopChoice(allColors[3], engine) { correct, finished ->
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

private fun handleStroopChoice(
    color: StroopColor,
    engine: StroopEngine,
    onUpdated: (correct: Boolean, isFinished: Boolean) -> Unit
) {
    val correct = engine.submitAnswer(color, timestampMillis = System.currentTimeMillis())
    val finished = engine.isFinished()
    onUpdated(correct, finished)
}
