package com.example.tictactoe.ui.focus

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoe.domain.focus.CognitiveBenchmark
import com.example.tictactoe.domain.focus.CognitivePillars
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.ui.theme.LocalGameColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CognitiveAnalyticsScreen(
    progress: PlayerProgress,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current

    val pillars = remember(progress) {
        val speedPct = CognitiveBenchmark.reactionTimePercentile(if (progress.bestReactionMs > 0) progress.bestReactionMs else 280L)
        val memoryPct = CognitiveBenchmark.workingMemoryPercentile(if (progress.bestSpatialSpan > 0) progress.bestSpatialSpan else 5)
        val inhibPct = CognitiveBenchmark.inhibitionPercentile(if (progress.bestInhibitionPercent > 0) progress.bestInhibitionPercent else 80)
        val flexPct = CognitiveBenchmark.flexibilityPercentile(if (progress.bestFlexibilitySeconds > 0) progress.bestFlexibilitySeconds else 24.0)
        val mathPct = CognitiveBenchmark.speedMathPercentile(if (progress.bestMathPerMinute > 0) progress.bestMathPerMinute else 15)

        CognitivePillars(
            speedPercentile = speedPct,
            memoryPercentile = memoryPct,
            inhibitionPercentile = inhibPct,
            flexibilityPercentile = flexPct,
            problemSolvingPercentile = mathPct
        )
    }

    val overallTier = CognitiveBenchmark.tierLabel(pillars.overallIndex)

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                    text = "COGNITIVE ANALYTICS",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Overall Score Card
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, colors.panelBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OVERALL COGNITIVE INDEX",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${pillars.overallIndex} / 100",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$overallTier (Worldwide Benchmark)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You outperform ${pillars.overallIndex}% of individuals across memory, speed, and executive control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }

            // 5-Axis Radar Chart
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, colors.panelBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "5-PILLAR COGNITIVE RADAR",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val radarScores = listOf(
                        pillars.speedPercentile.toFloat(),
                        pillars.memoryPercentile.toFloat(),
                        pillars.inhibitionPercentile.toFloat(),
                        pillars.flexibilityPercentile.toFloat(),
                        pillars.problemSolvingPercentile.toFloat()
                    )

                    val primaryColor = colors.primary
                    val borderColor = colors.panelBorder

                    Canvas(modifier = Modifier.size(240.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2.5f

                        // Draw 3 concentric polygon rings (33%, 66%, 100%)
                        for (ring in listOf(0.33f, 0.66f, 1.0f)) {
                            val ringPath = Path()
                            for (i in 0 until 5) {
                                val angle = (i * 2 * kotlin.math.PI / 5) - (kotlin.math.PI / 2)
                                val x = center.x + (radius * ring * cos(angle)).toFloat()
                                val y = center.y + (radius * ring * sin(angle)).toFloat()
                                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                            }
                            ringPath.close()
                            drawPath(ringPath, color = borderColor.copy(alpha = 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
                        }

                        // Draw User's Polygon
                        val userPath = Path()
                        for (i in 0 until 5) {
                            val scoreFraction = (radarScores[i] / 100f).coerceIn(0.1f, 1.0f)
                            val angle = (i * 2 * kotlin.math.PI / 5) - (kotlin.math.PI / 2)
                            val x = center.x + (radius * scoreFraction * cos(angle)).toFloat()
                            val y = center.y + (radius * scoreFraction * sin(angle)).toFloat()
                            if (i == 0) userPath.moveTo(x, y) else userPath.lineTo(x, y)
                        }
                        userPath.close()
                        drawPath(userPath, color = primaryColor.copy(alpha = 0.35f))
                        drawPath(userPath, color = primaryColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Speed • Memory • Inhibition • Flexibility • Logic",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }

            // Detailed Peer Percentile Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PERCENTILE BENCHMARK BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                BenchmarkRowCard(
                    icon = "⚡",
                    title = "Reaction Speed & Reflexes",
                    stat = "${if (progress.bestReactionMs > 0) progress.bestReactionMs else 280}ms",
                    percentile = pillars.speedPercentile,
                    comparison = "Faster than ${pillars.speedPercentile}% of population (avg 290ms)"
                )

                BenchmarkRowCard(
                    icon = "🧠",
                    title = "Working Memory Span",
                    stat = "${if (progress.bestSpatialSpan > 0) progress.bestSpatialSpan else 5} items",
                    percentile = pillars.memoryPercentile,
                    comparison = "Exceeds ${pillars.memoryPercentile}% of adult working memory buffers"
                )

                BenchmarkRowCard(
                    icon = "🛡️",
                    title = "Impulse & Inhibitory Control",
                    stat = "${if (progress.bestInhibitionPercent > 0) progress.bestInhibitionPercent else 82}%",
                    percentile = pillars.inhibitionPercentile,
                    comparison = "Top ${100 - pillars.inhibitionPercentile}% in suppressing automatic cognitive interference"
                )

                BenchmarkRowCard(
                    icon = "🔄",
                    title = "Task-Switching Flexibility",
                    stat = "Trail Test",
                    percentile = pillars.flexibilityPercentile,
                    comparison = "Mental shifting speed is in the top ${100 - pillars.flexibilityPercentile}%"
                )

                BenchmarkRowCard(
                    icon = "🧩",
                    title = "Mental Arithmetic & Logic",
                    stat = "${if (progress.bestMathPerMinute > 0) progress.bestMathPerMinute else 15} ops/m",
                    percentile = pillars.problemSolvingPercentile,
                    comparison = "Calculation throughput outperforms ${pillars.problemSolvingPercentile}% of peers"
                )
            }
        }
    }
}

@Composable
private fun BenchmarkRowCard(
    icon: String,
    title: String,
    stat: String,
    percentile: Int,
    comparison: String
) {
    val colors = LocalGameColors.current
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, colors.panelBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.panel),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = icon, fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Top ${100 - percentile}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comparison,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
        }
    }
}
