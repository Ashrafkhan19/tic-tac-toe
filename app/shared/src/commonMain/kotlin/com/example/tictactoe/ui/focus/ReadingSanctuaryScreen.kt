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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tictactoe.domain.focus.FocusXpRules
import com.example.tictactoe.domain.focus.ReadingArticle
import com.example.tictactoe.domain.focus.ReadingRepository
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun ReadingSanctuaryScreen(
    onArticleFinished: (articleId: String, scorePercent: Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    val repository = remember { ReadingRepository() }
    val articles = remember { repository.getAllArticles() }
    var selectedArticle by remember { mutableStateOf<ReadingArticle?>(null) }

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
        if (selectedArticle == null) {
            ArticleSelectionView(
                articles = articles,
                onSelectArticle = { selectedArticle = it },
                onBack = onBack
            )
        } else {
            ActiveReadingView(
                article = selectedArticle!!,
                repository = repository,
                onComplete = { score ->
                    onArticleFinished(selectedArticle!!.id, score)
                },
                onExitReading = { selectedArticle = null }
            )
        }
    }
}

@Composable
private fun ArticleSelectionView(
    articles: List<ReadingArticle>,
    onSelectArticle: (ReadingArticle) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current

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
                text = "DEEP READING SANCTUARY",
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Text(
            text = "Scroll-free text sprints followed by active recall to rebuild cognitive depth.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(articles) { article ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectArticle(article) }
                        .border(1.dp, colors.panelBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = colors.panel),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = article.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                            Text(
                                text = "${article.estimatedMinutes} min sprint",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveReadingView(
    article: ReadingArticle,
    repository: ReadingRepository,
    onComplete: (score: Int) -> Unit,
    onExitReading: () -> Unit
) {
    val colors = LocalGameColors.current
    var currentPage by remember { mutableStateOf(0) }
    var inQuizMode by remember { mutableStateOf(false) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    var quizResultScore by remember { mutableStateOf<Int?>(null) }

    val totalPages = article.pages.size

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExitReading,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Exit", color = colors.muted, fontSize = 12.sp)
                }
                Text(
                    text = if (inQuizMode) "ACTIVE RECALL QUIZ" else "PAGE ${currentPage + 1} OF $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = {
                    if (inQuizMode) 1f else (currentPage + 1).toFloat() / totalPages.toFloat()
                },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = colors.primary,
                trackColor = colors.panel
            )
        }

        if (!inQuizMode) {
            // Paginated Reading Content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .border(1.dp, colors.panelBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = colors.panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = article.pages[currentPage],
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurface,
                            lineHeight = 28.sp,
                            fontFamily = FontFamily.Default
                        )
                    }
                    Text(
                        text = "Static page • Zero scrolling • Pure focus",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Previous", color = colors.onSurface)
                    }
                }
                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                        } else {
                            inQuizMode = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(
                        text = if (currentPage < totalPages - 1) "Next Page" else "Start Recall Quiz",
                        color = colors.backgroundTop,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (quizResultScore == null) {
            // Active Recall Quiz Questions
            LazyColumn(
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(article.questions.indices.toList()) { qIndex ->
                    val question = article.questions[qIndex]
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, colors.panelBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = colors.panel),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Q${qIndex + 1}: ${question.prompt}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            question.options.forEachIndexed { optIndex, optText ->
                                val isSelected = userAnswers[qIndex] == optIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { userAnswers[qIndex] = optIndex }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { userAnswers[qIndex] = optIndex },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                    )
                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) colors.primary else colors.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Submit Button
            val allAnswered = article.questions.indices.all { userAnswers.containsKey(it) }
            Button(
                onClick = {
                    val answersList = article.questions.indices.map { userAnswers[it] ?: -1 }
                    quizResultScore = repository.evaluateQuiz(article.id, answersList)
                },
                enabled = allAnswered,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text(
                    text = "Submit Quiz & Evaluate",
                    color = colors.backgroundTop,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Quiz Result Card
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
                        text = "RECALL SCORE",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${quizResultScore}%",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val earnedXp = FocusXpRules.xpForDeepReading(quizResultScore!!)
                    Text(
                        text = if (quizResultScore!! >= 66) {
                            "Strong memory encoding! Retrieval practice solidified synaptic pathways (+${earnedXp} XP)."
                        } else {
                            "Good effort! Skimming is a habit that takes time to break (+${earnedXp} XP)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onComplete(quizResultScore!!) },
                        shape = RoundedCornerShape(10.dp),
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
