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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoe.domain.focus.DailyCircuitState
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun GymScreen(
    progress: PlayerProgress,
    dailyCircuit: DailyCircuitState,
    onOpenUrgeSurfer: () -> Unit,
    onOpenNeuroLab: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenFocusTimer: () -> Unit,
    onOpenStrategyGame: () -> Unit,
    onOpenSomaticReset: () -> Unit,
    onToggleSleepFoundation: () -> Unit,
    onToggleMovementFoundation: () -> Unit
) {
    val colors = LocalGameColors.current
    var activeScienceInsight by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.backgroundTop, colors.backgroundBottom)
                )
            )
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header with Streaks and Focus XP
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MIND GYM",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Cognitive Rehab & Attention Training",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(
                        label = "🔥 ${progress.streak.current}d Streak",
                        bgColor = colors.panel,
                        textColor = colors.onSurface
                    )
                    StatPill(
                        label = "⚡ ${progress.xp} XP",
                        bgColor = colors.primary.copy(alpha = 0.18f),
                        textColor = colors.primary
                    )
                }
            }

            // 2. Instant SOS Urge Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUrgeSurfer() }
                    .border(1.5.dp, Color(0xFFE53935).copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935).copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFE53935).copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚨", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Craving a scroll? Surf the urge",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "60-second physiological sigh to stop dopamine cravings",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 3. Daily Foundations (Body & Brain)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BODY & BRAIN FOUNDATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap ℹ️ for neuroscience",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FoundationCard(
                        title = "😴 7–8h Deep Sleep",
                        subtitle = if (dailyCircuit.sleepLogged) "Logged (+15 XP)" else "Tap to log",
                        isCompleted = dailyCircuit.sleepLogged,
                        modifier = Modifier.weight(1f),
                        onToggle = onToggleSleepFoundation,
                        onInfo = {
                            activeScienceInsight = "SLOW-WAVE SLEEP & MEMORY CONSOLIDATION\n\nRecent 2024 neuroimaging shows that during deep slow-wave sleep, cortical slow oscillations (<1Hz) synchronize with hippocampal ripples to transfer waking memories into permanent neocortical storage while the glymphatic system flushes metabolic waste from the prefrontal cortex."
                        }
                    )
                    FoundationCard(
                        title = "🏃 20m Aerobic Movement",
                        subtitle = if (dailyCircuit.movementLogged) "Logged (+15 XP)" else "Tap to log",
                        isCompleted = dailyCircuit.movementLogged,
                        modifier = Modifier.weight(1f),
                        onToggle = onToggleMovementFoundation,
                        onInfo = {
                            activeScienceInsight = "AEROBIC MOVEMENT & BDNF NEUROGENESIS\n\nMeta-analyses (Ye et al. 2024) confirm that 20 minutes of moderate aerobic exercise triggers systemic release of Brain-Derived Neurotrophic Factor (BDNF). This stimulates neurogenesis and immediately elevates executive function, working memory, and focus for 2–3 hours."
                        }
                    )
                }

                // Somatic Body Reset Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSomaticReset() }
                        .border(1.dp, Color(0xFF66BB6A).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF66BB6A).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🌿", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Somatic Body Reset (2 min)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFA5D6A7),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Guided muscle tension & release to purge physical screen cortisol",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 4. Today's Focus Circuit Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.panelBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S FOCUS CIRCUIT",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (dailyCircuit.isComplete) "100% COMPLETE" else "12 MIN WORKOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dailyCircuit.isComplete) colors.primary else colors.muted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val progressValue = when {
                        dailyCircuit.isComplete -> 1f
                        dailyCircuit.neuroDrillCompleted -> 0.66f
                        dailyCircuit.urgeWarmupCompleted -> 0.33f
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = colors.primary,
                        trackColor = colors.backgroundTop
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    CircuitStepRow(
                        step = 1,
                        name = "Urge Breathwork (60s)",
                        isDone = dailyCircuit.urgeWarmupCompleted
                    )
                    CircuitStepRow(
                        step = 2,
                        name = "Stroop Neuro Drill (3m)",
                        isDone = dailyCircuit.neuroDrillCompleted
                    )
                    CircuitStepRow(
                        step = 3,
                        name = "Deep Reading or Strategy Match (6m)",
                        isDone = dailyCircuit.deepActivityCompleted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!dailyCircuit.isComplete) {
                        Button(
                            onClick = {
                                when {
                                    !dailyCircuit.urgeWarmupCompleted -> onOpenUrgeSurfer()
                                    !dailyCircuit.neuroDrillCompleted -> onOpenNeuroLab()
                                    else -> onOpenReading()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text(
                                text = when {
                                    !dailyCircuit.urgeWarmupCompleted -> "Start Step 1: Urge Breathwork"
                                    !dailyCircuit.neuroDrillCompleted -> "Start Step 2: Stroop Drill"
                                    else -> "Start Step 3: Deep Reading"
                                },
                                color = colors.backgroundTop,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "🎉 Today's circuit complete! Your focus streak is preserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 5. Open Gym Modules
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "OPEN GYM MODULES",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymModuleCard(
                        icon = "🧠",
                        title = "Strategic Gym",
                        desc = "Tic-Tac-Toe & Ultimate",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenStrategyGame
                    )
                    GymModuleCard(
                        icon = "⚡",
                        title = "Neuro Lab",
                        desc = "Stroop inhibitory control",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenNeuroLab
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymModuleCard(
                        icon = "📖",
                        title = "Deep Reading",
                        desc = "Scroll-free text & recall",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenReading
                    )
                    GymModuleCard(
                        icon = "⏱️",
                        title = "Focus Timer",
                        desc = "Deep work & distraction log",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenFocusTimer
                    )
                }
            }
        }

        // Science Insight Dialog
        if (activeScienceInsight != null) {
            AlertDialog(
                onDismissRequest = { activeScienceInsight = null },
                confirmButton = {
                    TextButton(onClick = { activeScienceInsight = null }) {
                        Text("Got it", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text(
                        text = "Neuroscience Foundation",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                },
                text = {
                    Text(
                        text = activeScienceInsight!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                },
                containerColor = colors.backgroundTop,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FoundationCard(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onInfo: () -> Unit
) {
    val colors = LocalGameColors.current
    Card(
        modifier = modifier
            .clickable { onToggle() }
            .border(
                1.dp,
                if (isCompleted) colors.primary.copy(alpha = 0.5f) else colors.panelBorder,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) colors.primary.copy(alpha = 0.12f) else colors.panel
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "ℹ️",
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onInfo() }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isCompleted) colors.primary else colors.muted
            )
        }
    }
}

@Composable
private fun CircuitStepRow(
    step: Int,
    name: String,
    isDone: Boolean
) {
    val colors = LocalGameColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Step $step: $name",
            style = MaterialTheme.typography.bodySmall,
            color = if (isDone) colors.primary else colors.onSurface
        )
        Text(
            text = if (isDone) "✓" else "○",
            color = if (isDone) colors.primary else colors.muted,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GymModuleCard(
    icon: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, colors.panelBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.panel),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = icon, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted
            )
        }
    }
}
