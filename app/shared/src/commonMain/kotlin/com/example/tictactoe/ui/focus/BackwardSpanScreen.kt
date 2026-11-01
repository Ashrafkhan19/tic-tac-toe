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
import com.example.tictactoe.domain.focus.BackwardSpanEngine
import com.example.tictactoe.domain.focus.CognitiveBenchmark
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

@Composable
fun BackwardSpanScreen(
    onComplete: (maxSpan: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { BackwardSpanEngine(startingSpan = 3) }

    var isDemonstrating by remember { mutableStateOf(true) }
    var activeDigit by remember { mutableStateOf<Int?>(null) }
    var isGameOver by remember { mutableStateOf(false) }
    val enteredDigits = remember { mutableStateListOf<Int>() }
    var currentRound by remember { mutableStateOf(0) }

    LaunchedEffect(currentRound, isGameOver) {
        if (!isGameOver) {
            isDemonstrating = true
            enteredDigits.clear()
            delay(500L)
            val seq = engine.currentSequence()
            for (num in seq) {
                activeDigit = num
                delay(750L)
                activeDigit = null
                delay(250L)
            }
            isDemonstrating = false
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
        if (!isGameOver) {
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
                            text = "BACKWARD MEMORY SPAN",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target Span: ${engine.currentSpan} digits",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Strikes: ${engine.strikes}/2",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (engine.strikes > 0) Color(0xFFE53935) else colors.muted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Central Display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, colors.panelBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = colors.panel),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDemonstrating) {
                            Text(
                                text = if (activeDigit != null) "$activeDigit" else "...",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.primary
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ENTER IN REVERSE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (enteredDigits.isEmpty()) "_ ".repeat(engine.currentSequence().size) else enteredDigits.joinToString(" "),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                            }
                        }
                    }
                }

                // Numerical Keypad (1..9, Backspace, Submit)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = listOf(
                        listOf(1, 2, 3),
                        listOf(4, 5, 6),
                        listOf(7, 8, 9)
                    )
                    rows.forEach { rowNums ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowNums.forEach { num ->
                                Button(
                                    onClick = {
                                        if (!isDemonstrating && enteredDigits.size < engine.currentSequence().size) {
                                            enteredDigits.add(num)
                                        }
                                    },
                                    enabled = !isDemonstrating,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.panel),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("$num", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                                }
                            }
                        }
                    }

                    // Bottom Row: Clear & Submit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (enteredDigits.isNotEmpty()) enteredDigits.removeLast() },
                            enabled = !isDemonstrating && enteredDigits.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("⌫ Del", color = colors.muted)
                        }
                        Button(
                            onClick = {
                                val success = engine.submitReverseSequence(enteredDigits.toList())
                                if (engine.isGameOver) {
                                    isGameOver = true
                                } else {
                                    currentRound++
                                }
                            },
                            enabled = !isDemonstrating && enteredDigits.size == engine.currentSequence().size,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier.weight(2f).height(46.dp)
                        ) {
                            Text("Submit Reverse", color = colors.backgroundTop, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Results Card
            val percentile = CognitiveBenchmark.workingMemoryPercentile(engine.maxSpanAchieved)
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
                        text = "BACKWARD SPAN COMPLETE",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${engine.maxSpanAchieved}",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Reverse Working Memory Span",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Top ${100 - percentile}% of adults • Backward recall directly trains active mental schema inversion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onComplete(engine.maxSpanAchieved) },
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
