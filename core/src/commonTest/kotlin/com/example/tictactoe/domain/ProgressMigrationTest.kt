package com.example.tictactoe.domain

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.progress.DifficultyStats
import com.example.tictactoe.domain.progress.GameResult
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.StreakState
import com.example.tictactoe.domain.progress.XpRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressMigrationTest {

    @Test
    fun legacyV1ProgressJsonDeserializesCleanly() {
        // v1 json format with "stats" containing Easy/Medium/Unbeatable
        val v1Json = """
            {
              "version": 1,
              "xp": 150,
              "stats": {
                "Easy": { "wins": 3, "draws": 1, "losses": 0 },
                "Unbeatable": { "wins": 1, "draws": 2, "losses": 5 }
              },
              "streak": { "current": 2, "best": 4 }
            }
        """.trimIndent()

        val progress = PlayerProgress.fromJson(v1Json)
        assertEquals(2, progress.version)
        assertEquals(150, progress.xp)
        assertEquals(3, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Easy).wins)
        assertEquals(1, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Easy).draws)
        assertEquals(0, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Easy).losses)
        assertEquals(1, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable).wins)
        assertEquals(2, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable).draws)
        assertEquals(5, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable).losses)
        assertEquals(0, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Medium).wins)
        assertEquals(2, progress.streak.current)
        assertEquals(4, progress.streak.best)
    }

    @Test
    fun legacyV1ProgressWithoutStatsUpgradesVersionAndPreservesData() {
        val v1Json = """
            {
              "version": 1,
              "xp": 80,
              "streak": { "current": 1, "best": 3 }
            }
        """.trimIndent()

        val progress = PlayerProgress.fromJson(v1Json)
        assertEquals(2, progress.version)
        assertEquals(80, progress.xp)
        assertTrue(progress.variantStats.isEmpty())
        assertEquals(1, progress.streak.current)
    }

    @Test
    fun v1JsonWithUnknownDifficultyDropsUnknownAndMigratesKnown() {
        val v1Json = """
            {
              "version": 1,
              "xp": 95,
              "stats": {
                "Nightmare": { "wins": 10, "draws": 0, "losses": 0 },
                "Medium": { "wins": 2, "draws": 1, "losses": 1 }
              }
            }
        """.trimIndent()

        val progress = PlayerProgress.fromJson(v1Json)
        assertEquals(2, progress.version)
        assertEquals(95, progress.xp)
        assertEquals(2, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Medium).wins)
        assertEquals(1, progress.variantStats.size)
    }

    @Test
    fun v2ProgressJsonRoundTripsCleanly() {
        val v2Json = """
            {
              "version": 2,
              "xp": 500,
              "variantStats": {
                "CLASSIC_3X3:Easy": { "wins": 5, "draws": 0, "losses": 1 },
                "GRID_4X4:Unbeatable": { "wins": 2, "draws": 1, "losses": 3 },
                "ULTIMATE:Medium": { "wins": 1, "draws": 0, "losses": 0 }
              },
              "streak": { "current": 3, "best": 5, "freezes": 1 }
            }
        """.trimIndent()

        val progress = PlayerProgress.fromJson(v2Json)
        assertEquals(2, progress.version)
        assertEquals(500, progress.xp)
        assertEquals(5, progress.statsFor(GameVariant.CLASSIC_3X3, Difficulty.Easy).wins)
        assertEquals(2, progress.statsFor(GameVariant.GRID_4X4, Difficulty.Unbeatable).wins)
        assertEquals(1, progress.statsFor(GameVariant.ULTIMATE, Difficulty.Medium).wins)
        assertEquals(3, progress.streak.current)
        assertEquals(1, progress.streak.freezes)
    }

    @Test
    fun xpAwardsScaleWithVariantComplexity() {
        // Unbeatable Wins
        val win3x3 = XpRules.forGame(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable, GameResult.Win)
        val win4x4 = XpRules.forGame(GameVariant.GRID_4X4, Difficulty.Unbeatable, GameResult.Win)
        val win5x5 = XpRules.forGame(GameVariant.GRID_5X5, Difficulty.Unbeatable, GameResult.Win)
        val winUltimate = XpRules.forGame(GameVariant.ULTIMATE, Difficulty.Unbeatable, GameResult.Win)

        assertEquals(40, win3x3)
        assertEquals(60, win4x4)
        assertEquals(80, win5x5)
        assertEquals(120, winUltimate)

        // Medium Wins
        assertEquals(20, XpRules.forGame(GameVariant.CLASSIC_3X3, Difficulty.Medium, GameResult.Win))
        assertEquals(35, XpRules.forGame(GameVariant.GRID_4X4, Difficulty.Medium, GameResult.Win))
        assertEquals(50, XpRules.forGame(GameVariant.GRID_5X5, Difficulty.Medium, GameResult.Win))
        assertEquals(80, XpRules.forGame(GameVariant.ULTIMATE, Difficulty.Medium, GameResult.Win))

        // Easy Wins
        assertEquals(10, XpRules.forGame(GameVariant.CLASSIC_3X3, Difficulty.Easy, GameResult.Win))
        assertEquals(20, XpRules.forGame(GameVariant.GRID_4X4, Difficulty.Easy, GameResult.Win))
        assertEquals(30, XpRules.forGame(GameVariant.GRID_5X5, Difficulty.Easy, GameResult.Win))
        assertEquals(50, XpRules.forGame(GameVariant.ULTIMATE, Difficulty.Easy, GameResult.Win))

        // Losses
        assertEquals(1, XpRules.forGame(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable, GameResult.Loss))
        assertEquals(2, XpRules.forGame(GameVariant.GRID_4X4, Difficulty.Unbeatable, GameResult.Loss))
        assertEquals(3, XpRules.forGame(GameVariant.GRID_5X5, Difficulty.Unbeatable, GameResult.Loss))
        assertEquals(5, XpRules.forGame(GameVariant.ULTIMATE, Difficulty.Unbeatable, GameResult.Loss))

        // Draws
        assertEquals(15, XpRules.forGame(GameVariant.CLASSIC_3X3, Difficulty.Unbeatable, GameResult.Draw))
        assertEquals(20, XpRules.forGame(GameVariant.GRID_4X4, Difficulty.Unbeatable, GameResult.Draw))
        assertEquals(30, XpRules.forGame(GameVariant.GRID_5X5, Difficulty.Unbeatable, GameResult.Draw))
        assertEquals(40, XpRules.forGame(GameVariant.ULTIMATE, Difficulty.Unbeatable, GameResult.Draw))
    }

    @Test
    fun backwardsCompatibleForGameCallsWorkWithDefaultVariant() {
        assertEquals(10, XpRules.forGame(Difficulty.Easy, GameResult.Win))
        assertEquals(20, XpRules.forGame(Difficulty.Medium, GameResult.Win))
        assertEquals(40, XpRules.forGame(Difficulty.Unbeatable, GameResult.Win))
        assertEquals(3, XpRules.forGame(Difficulty.Easy, GameResult.Draw))
        assertEquals(1, XpRules.forGame(Difficulty.Easy, GameResult.Loss))
    }

    @Test
    fun recordGameUpdatesVariantStatsAndXpCorrectly() {
        val initial = PlayerProgress(xp = 100)
        val updated = initial.recordGame(GameVariant.GRID_4X4, Difficulty.Medium, GameResult.Win)

        assertEquals(135, updated.xp) // 100 + 35
        assertEquals(1, updated.statsFor(GameVariant.GRID_4X4, Difficulty.Medium).wins)
        assertEquals(0, updated.statsFor(GameVariant.GRID_4X4, Difficulty.Medium).draws)
        assertEquals(0, updated.statsFor(GameVariant.GRID_4X4, Difficulty.Medium).losses)

        // Subsequent game in same variant and difficulty accumulates
        val updatedAgain = updated.recordGame(GameVariant.GRID_4X4, Difficulty.Medium, GameResult.Draw)
        assertEquals(145, updatedAgain.xp) // 135 + 10
        assertEquals(1, updatedAgain.statsFor(GameVariant.GRID_4X4, Difficulty.Medium).wins)
        assertEquals(1, updatedAgain.statsFor(GameVariant.GRID_4X4, Difficulty.Medium).draws)
    }

    @Test
    fun backwardsCompatibleStatsPropertyExposesClassicStats() {
        val progress = PlayerProgress()
            .recordGame(GameVariant.CLASSIC_3X3, Difficulty.Medium, GameResult.Win)
            .recordGame(GameVariant.GRID_5X5, Difficulty.Easy, GameResult.Win)

        assertEquals(1, progress.stats[Difficulty.Medium]?.wins)
        assertNull(progress.stats[Difficulty.Easy]) // Only classic games are in stats
        assertEquals(1, progress.statsFor(GameVariant.GRID_5X5, Difficulty.Easy).wins)
    }
}
