package com.example.tictactoe.data

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.progress.DifficultyStats
import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.ProgressRepository
import com.example.tictactoe.domain.progress.StreakState
import com.russhwolf.settings.Settings
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Persists [PlayerProgress] as a single JSON string that always carries its `version`.
 *
 * Decoding is tolerant per field, so one bad entry never wipes the rest: unknown fields and unknown
 * difficulty keys are dropped, an unreadable number keeps its default, and an unreadable
 * `lastCompleted` becomes null while the streak counts are kept. Whenever anything had to be dropped,
 * or the blob is not a JSON object at all, the raw string is first copied to [CORRUPT_KEY] (the first
 * such copy is kept) so the next save cannot destroy the only record of it.
 */
class SettingsProgressRepository(
    private val settings: Settings,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : ProgressRepository {

    override fun load(): PlayerProgress {
        val raw = settings.getStringOrNull(KEY) ?: return PlayerProgress()
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonObject
        if (root == null) {
            backUp(raw)
            return PlayerProgress()
        }
        val decoder = TolerantDecoder()
        val progress = decoder.progress(root)
        if (decoder.lossy) backUp(raw)
        return progress
    }

    override fun save(progress: PlayerProgress) {
        settings.putString(KEY, json.encodeToString(PlayerProgress.serializer(), progress))
    }

    private fun backUp(raw: String) {
        if (settings.getStringOrNull(CORRUPT_KEY) == null) settings.putString(CORRUPT_KEY, raw)
    }

    /** Reads each known field on its own, recording whether any present value had to be discarded. */
    private class TolerantDecoder {
        var lossy = false
            private set

        private val defaults = PlayerProgress()

        fun progress(o: JsonObject): PlayerProgress {
            val version = int(o, "version") ?: defaults.version
            val xp = int(o, "xp") ?: defaults.xp
            val streak = streak(o["streak"])
            val variantStats = when {
                o.containsKey("variantStats") -> variantStats(o["variantStats"])
                o.containsKey("stats") -> stats(o["stats"]).mapKeys { "${GameVariant.CLASSIC_3X3.name}:${it.key.name}" }
                else -> emptyMap()
            }
            return PlayerProgress(
                version = version,
                xp = xp,
                variantStats = variantStats,
                streak = streak,
            )
        }

        private fun variantStats(element: JsonElement?): Map<String, DifficultyStats> {
            if (element == null) return emptyMap()
            val o = element as? JsonObject ?: return dropped(emptyMap())
            return buildMap {
                for ((key, value) in o) {
                    val entry = value as? JsonObject
                    if (entry == null) {
                        lossy = true
                        continue
                    }
                    put(
                        key,
                        DifficultyStats(
                            wins = int(entry, "wins") ?: 0,
                            draws = int(entry, "draws") ?: 0,
                            losses = int(entry, "losses") ?: 0,
                        ),
                    )
                }
            }
        }

        private fun stats(element: JsonElement?): Map<Difficulty, DifficultyStats> {
            if (element == null) return emptyMap()
            val o = element as? JsonObject ?: return dropped(emptyMap())
            return buildMap {
                for ((name, value) in o) {
                    val difficulty = Difficulty.entries.firstOrNull { it.name == name }
                    val entry = value as? JsonObject
                    if (difficulty == null || entry == null) {
                        lossy = true
                        continue
                    }
                    put(
                        difficulty,
                        DifficultyStats(
                            wins = int(entry, "wins") ?: 0,
                            draws = int(entry, "draws") ?: 0,
                            losses = int(entry, "losses") ?: 0,
                        ),
                    )
                }
            }
        }

        private fun streak(element: JsonElement?): StreakState {
            if (element == null) return StreakState()
            val o = element as? JsonObject ?: return dropped(StreakState())
            return StreakState(
                current = int(o, "current") ?: 0,
                best = int(o, "best") ?: 0,
                freezes = int(o, "freezes") ?: 0,
                lastCompleted = date(o, "lastCompleted"),
            )
        }

        /** Null when absent or JSON null; a present but unreadable value is dropped. */
        private fun int(o: JsonObject, key: String): Int? {
            val value = o[key] ?: return null
            if (value is JsonNull) return null
            return (value as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull ?: dropped(null)
        }

        private fun date(o: JsonObject, key: String): LocalDate? {
            val value = o[key] ?: return null
            if (value is JsonNull) return null
            val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return dropped(null)
            return runCatching { LocalDate.parse(text) }.getOrNull() ?: dropped(null)
        }

        private fun <T> dropped(fallback: T): T {
            lossy = true
            return fallback
        }
    }

    private companion object {
        const val KEY = "player_progress_v1"
        const val CORRUPT_KEY = "player_progress_v1_corrupt"
    }
}
