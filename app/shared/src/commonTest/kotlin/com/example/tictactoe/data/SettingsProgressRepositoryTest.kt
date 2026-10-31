package com.example.tictactoe.data

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.progress.DifficultyStats
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.StreakState
import com.russhwolf.settings.MapSettings
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsProgressRepositoryTest {
    private val key = "player_progress_v1"

    private val sample = PlayerProgress(
        xp = 345,
        stats = mapOf(
            Difficulty.Easy to DifficultyStats(wins = 3, draws = 1, losses = 0),
            Difficulty.Medium to DifficultyStats(wins = 2, draws = 2, losses = 1),
            Difficulty.Unbeatable to DifficultyStats(wins = 0, draws = 4, losses = 9),
        ),
        streak = StreakState(current = 4, best = 7, freezes = 1, lastCompleted = LocalDate(2026, 10, 30)),
    )

    @Test
    fun roundTripsAllFields() {
        val repo = SettingsProgressRepository(MapSettings())
        repo.save(sample)
        assertEquals(sample, repo.load())
    }

    @Test
    fun roundTripsStatsMapWithAllDifficulties() {
        val repo = SettingsProgressRepository(MapSettings())
        repo.save(sample)
        val loaded = repo.load()
        assertEquals(setOf(Difficulty.Easy, Difficulty.Medium, Difficulty.Unbeatable), loaded.stats.keys)
        assertEquals(DifficultyStats(2, 2, 1), loaded.stats[Difficulty.Medium])
    }

    @Test
    fun roundTripsNullAndNonNullDate() {
        val repo = SettingsProgressRepository(MapSettings())
        repo.save(PlayerProgress(streak = StreakState(lastCompleted = null)))
        assertEquals(null, repo.load().streak.lastCompleted)
        repo.save(sample)
        assertEquals(LocalDate(2026, 10, 30), repo.load().streak.lastCompleted)
    }

    @Test
    fun storesDateAsIsoString() {
        val settings = MapSettings()
        SettingsProgressRepository(settings).save(sample)
        val raw = settings.getString(key, "")
        assertTrue(raw.contains("\"2026-10-30\""), raw)
    }

    @Test
    fun emptyStorageGivesDefaults() {
        assertEquals(PlayerProgress(), SettingsProgressRepository(MapSettings()).load())
    }

    @Test
    fun garbageGivesDefaultsWithoutThrowing() {
        val settings = MapSettings()
        settings.putString(key, "{not json")
        assertEquals(PlayerProgress(), SettingsProgressRepository(settings).load())
    }

    @Test
    fun badDateGivesDefaultsWithoutThrowing() {
        val settings = MapSettings()
        settings.putString(key, """{"streak":{"lastCompleted":"not-a-date"}}""")
        assertEquals(PlayerProgress(), SettingsProgressRepository(settings).load())
    }

    @Test
    fun unknownFieldAndHigherVersionLoadKnownFields() {
        val settings = MapSettings()
        settings.putString(key, """{"version":9,"xp":42,"futureField":{"a":1},"stats":{"Easy":{"wins":1}}}""")
        val loaded = SettingsProgressRepository(settings).load()
        assertEquals(42, loaded.xp)
        assertEquals(9, loaded.version)
        assertEquals(DifficultyStats(wins = 1), loaded.stats[Difficulty.Easy])
    }

    @Test
    fun savedValueSurvivesNewInstance() {
        val settings = MapSettings()
        SettingsProgressRepository(settings).save(sample)
        assertEquals(sample, SettingsProgressRepository(settings).load())
    }
}
