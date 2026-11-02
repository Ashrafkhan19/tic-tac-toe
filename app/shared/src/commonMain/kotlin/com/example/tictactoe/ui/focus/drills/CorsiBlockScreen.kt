package com.example.tictactoe.ui.focus.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.example.tictactoe.domain.focus.CorsiBlockEngine
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

@Composable
fun CorsiBlockScreen(
    onComplete: (maxSpan: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { CorsiBlockEngine(startingSpan = 3) }

    var isDemonstrating by remember { mutableStateOf(true) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    var isGameOver by remember { mutableStateOf(false) }
    val userTaps = remember { mutableStateListOf<Int>() }
    var currentRound by remember { mutableStateOf(0) }

    // Flash sequence during demonstration phase
    LaunchedEffect(currentRound, isGameOver) {
        if (!isGameOver) {
            isDemonstrating = true
            userTaps.clear()
            delay(500L)
            val seq = engine.currentSequence()
            for (tileIndex in seq) {
                highlightedIndex = tileIndex
                delay(650L)
                highlightedIndex = null
                delay(200L)
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
                            text = "CORSI BLOCK MATRIX",
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
                            text = "Target Span: ${engine.currentSpan} blocks",
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

                // 3x3 Matrix
                Column(
                    modifier = Modifier.size(280.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (col in 0 until 3) {
                                val tileIndex = row * 3 + col
                                val isLit = highlightedIndex == tileIndex
                                val isUserTapped = userTaps.contains(tileIndex)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            when {
                                                isLit -> colors.primary
                                                isUserTapped -> colors.primary.copy(alpha = 0.35f)
                                                else -> colors.panel
                                            },
                                            RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isLit) colors.primary else colors.panelBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable(enabled = !isDemonstrating) {
                                            userTaps.add(tileIndex)
                                            if (userTaps.size == engine.currentSequence().size) {
                                                val success = engine.submitSequence(userTaps.toList())
                                                if (engine.isGameOver) {
                                                    isGameOver = true
                                                } else {
                                                    currentRound++
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUserTapped) {
                                        Text(
                                            text = "${userTaps.indexOf(tileIndex) + 1}",
                                            color = colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Status text
                Column(
                    modifier = Modifier.padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isDemonstrating) "Watch the pattern carefully..." else "Repeat the sequence: ${userTaps.size} of ${engine.currentSequence().size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDemonstrating) colors.primary else colors.onSurface,
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
                        text = "SPATIAL SPAN TEST COMPLETE",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${engine.maxSpanAchieved}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Maximum Working Memory Span (Blocks)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (engine.maxSpanAchieved >= 6) {
                            "Above average visuospatial capacity! Your hippocampus and parietal cortex hold complex coordinate schemas effortlessly."
                        } else {
                            "Solid foundation! Regular spatial span drills directly expand working memory buffer limits."
                        },
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

