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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoe.domain.focus.FocusXpRules
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

@Composable
fun FocusTimerScreen(
    onSessionComplete: (durationMinutes: Int, distractions: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current

    var selectedDurationMinutes by remember { mutableStateOf(25) }
    var secondsRemaining by remember { mutableStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var distractionsLogged by remember { mutableStateOf(0) }
    var showDistractionToast by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            if (secondsRemaining == 0) {
                isRunning = false
                isFinished = true
            }
        }
    }

    LaunchedEffect(showDistractionToast) {
        if (showDistractionToast) {
            delay(2500L)
            showDistractionToast = false
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

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
                        text = "DEEP WORK SANCTUARY",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Preset selector (only visible before starting)
                if (!isRunning && secondsRemaining == selectedDurationMinutes * 60) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        PresetChip(
                            label = "5m Sprint",
                            selected = selectedDurationMinutes == 5,
                            onClick = {
                                selectedDurationMinutes = 5
                                secondsRemaining = 5 * 60
                            }
                        )
                        PresetChip(
                            label = "25m Pomodoro",
                            selected = selectedDurationMinutes == 25,
                            onClick = {
                                selectedDurationMinutes = 25
                                secondsRemaining = 25 * 60
                            }
                        )
                        PresetChip(
                            label = "50m Deep Block",
                            selected = selectedDurationMinutes == 50,
                            onClick = {
                                selectedDurationMinutes = 50
                                secondsRemaining = 50 * 60
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(30.dp))
                }

                // Central Timer Circle
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(colors.panel, CircleShape)
                        .border(4.dp, colors.primary.copy(alpha = if (isRunning) 0.85f else 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRunning) "Locked in • Single tasking" else "Ready to Focus",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                }

                // Distraction Logger
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showDistractionToast) {
                        Text(
                            text = "Awareness logged. Breathe and gently return to work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (isRunning) {
                        OutlinedButton(
                            onClick = {
                                distractionsLogged++
                                showDistractionToast = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = "⚡ Noticed Distraction Urge (${distractionsLogged})",
                                color = colors.onSurface,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Primary Play / Pause / Reset Controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isRunning) {
                        OutlinedButton(
                            onClick = { isRunning = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Pause", color = colors.onSurface)
                        }
                    } else {
                        Button(
                            onClick = { isRunning = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text(
                                text = if (secondsRemaining < selectedDurationMinutes * 60) "Resume" else "Start Session",
                                color = colors.backgroundTop,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (secondsRemaining < selectedDurationMinutes * 60) {
                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                secondsRemaining = selectedDurationMinutes * 60
                                distractionsLogged = 0
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Reset", color = colors.muted)
                        }
                    }
                }
            }
        } else {
            // Session Completion Card
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
                        text = "SESSION COMPLETE",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${selectedDurationMinutes} min",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Unbroken Deep Focus",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Distraction urges acknowledged: $distractionsLogged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val earnedXp = FocusXpRules.xpForFocusSession(selectedDurationMinutes, distractionsLogged)
                    Text(
                        text = "Focus reward: +$earnedXp XP",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onSessionComplete(selectedDurationMinutes, distractionsLogged) },
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

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) colors.primary.copy(alpha = 0.18f) else colors.panel
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) colors.primary else colors.panelBorder
        ),
        modifier = Modifier.height(34.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) colors.primary else colors.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

