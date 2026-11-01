package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadingRepositoryTest {
    @Test
    fun defaultArticles_areAvailableAndNonEmpty() {
        val repo = ReadingRepository()
        val articles = repo.getAllArticles()
        assertTrue(articles.isNotEmpty())
        
        val first = articles.first()
        assertTrue(first.pages.isNotEmpty())
        assertTrue(first.questions.isNotEmpty())
    }

    @Test
    fun evaluateQuiz_calculatesAccuratePercentage() {
        val repo = ReadingRepository()
        val article = repo.getAllArticles().first()
        val correctAnswers = article.questions.map { it.correctIndex }
        
        val score = repo.evaluateQuiz(article.id, correctAnswers)
        assertEquals(100, score)

        val wrongAnswers = article.questions.map { (it.correctIndex + 1) % it.options.size }
        val zeroScore = repo.evaluateQuiz(article.id, wrongAnswers)
        assertEquals(0, zeroScore)
    }

    @Test
    fun evaluateQuiz_handlesEmptyOrUnknownArticleGracefully() {
        val repo = ReadingRepository()
        val score = repo.evaluateQuiz("unknown-id", listOf(0, 1))
        assertEquals(0, score)
    }
}

