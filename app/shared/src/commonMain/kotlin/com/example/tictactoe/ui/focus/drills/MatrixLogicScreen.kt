package com.example.tictactoe.ui.focus.drills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.tictactoe.domain.focus.CognitiveBenchmark
import com.example.tictactoe.domain.focus.MatrixLogicEngine
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun MatrixLogicScreen(
    onComplete: (scorePercent: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val engine = remember { MatrixLogicEngine() }
    val puzzles = remember { engine.allPuzzles }

    var currentPuzzleIndex by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val puzzle = puzzles[currentPuzzleIndex]

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
                            text = "VISUAL MATRIX REASONING",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { (currentPuzzleIndex + 1).toFloat() / puzzles.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = colors.primary,
                        trackColor = colors.panel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Puzzle ${currentPuzzleIndex + 1} of ${puzzles.size}: ${puzzle.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }

                // 2x2 Matrix Display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(1.dp, colors.panelBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = colors.panel),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MatrixCell(icon = puzzle.gridIcons[0], modifier = Modifier.weight(1f))
                            MatrixCell(icon = puzzle.gridIcons[1], modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MatrixCell(icon = puzzle.gridIcons[2], modifier = Modifier.weight(1f))
                            MatrixCell(icon = puzzle.gridIcons[3], isMissing = true, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Options Grid (4 Choices)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (!isSubmitted) "Select the missing shape:" else puzzle.ruleDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!isSubmitted) colors.muted else colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 0 until 2) {
                            val isCorrect = i == puzzle.correctOptionIndex
                            Button(
                                onClick = {
                                    if (!isSubmitted) {
                                        selectedOption = i
                                        isSubmitted = true
                                        if (isCorrect) correctCount++
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isSubmitted && isCorrect -> Color(0xFF43A047)
                                        isSubmitted && selectedOption == i -> Color(0xFFE53935)
                                        else -> colors.panel
                                    }
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.panelBorder),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text(puzzle.options[i], fontSize = 20.sp, color = colors.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 2 until 4) {
                            val isCorrect = i == puzzle.correctOptionIndex
                            Button(
                                onClick = {
                                    if (!isSubmitted) {
                                        selectedOption = i
                                        isSubmitted = true
                                        if (isCorrect) correctCount++
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isSubmitted && isCorrect -> Color(0xFF43A047)
                                        isSubmitted && selectedOption == i -> Color(0xFFE53935)
                                        else -> colors.panel
                                    }
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.panelBorder),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text(puzzle.options[i], fontSize = 20.sp, color = colors.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isSubmitted) {
                        Button(
                            onClick = {
                                isSubmitted = false
                                selectedOption = null
                                if (currentPuzzleIndex < puzzles.lastIndex) {
                                    currentPuzzleIndex++
                                } else {
                                    isFinished = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = if (currentPuzzleIndex < puzzles.lastIndex) "Next Puzzle" else "See Results",
                                color = colors.backgroundTop,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Results Card
            val scorePercent = (correctCount * 100) / puzzles.size
            val percentile = CognitiveBenchmark.problemSolvingPercentile(scorePercent)
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
                        text = "FLUID REASONING COMPLETE",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$scorePercent%",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Abstract Pattern Deduction",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Top ${100 - percentile}% of peers worldwide • Non-verbal fluid reasoning actively exercises parietal and prefrontal circuits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onComplete(scorePercent) },
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
private fun MatrixCell(
    icon: String,
    isMissing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalGameColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isMissing) colors.primary.copy(alpha = 0.15f) else colors.backgroundTop, RoundedCornerShape(12.dp))
            .border(1.dp, if (isMissing) colors.primary else colors.panelBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isMissing) colors.primary else colors.onSurface
        )
    }
}

