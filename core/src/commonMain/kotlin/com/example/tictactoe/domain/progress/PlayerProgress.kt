package com.example.tictactoe.domain.progress

import com.example.tictactoe.domain.model.Difficulty
import com.example.tictactoe.domain.model.GameVariant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class GameResult { Win, Draw, Loss }

@Serializable
data class DifficultyStats(
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
) {
    fun record(result: GameResult): DifficultyStats = when (result) {
        GameResult.Win -> copy(wins = wins + 1)
        GameResult.Draw -> copy(draws = draws + 1)
        GameResult.Loss -> copy(losses = losses + 1)
    }
}

@Serializable
data class PlayerProgress(
    val version: Int = 2,
    val xp: Int = 0,
    val variantStats: Map<String, DifficultyStats> = emptyMap(),
    val streak: StreakState = StreakState(),
) {
    /** Backwards-compatibility computed view of classic 3x3 stats. */
    val stats: Map<Difficulty, DifficultyStats>
        get() = Difficulty.entries.mapNotNull { d ->
            variantStats["${GameVariant.CLASSIC_3X3.name}:${d.name}"]?.let { d to it }
        }.toMap()

    /** Secondary constructor for backwards compatibility with v1 callers passing `stats`. */
    constructor(
        stats: Map<Difficulty, DifficultyStats>,
        xp: Int = 0,
        streak: StreakState = StreakState(),
        version: Int = 2,
    ) : this(
        version = version,
        xp = xp,
        variantStats = stats.mapKeys { "${GameVariant.CLASSIC_3X3.name}:${it.key.name}" },
        streak = streak,
    )

    fun statsFor(variant: GameVariant, difficulty: Difficulty): DifficultyStats =
        variantStats["${variant.name}:${difficulty.name}"] ?: DifficultyStats()

    fun recordGame(variant: GameVariant, difficulty: Difficulty, result: GameResult): PlayerProgress {
        val key = "${variant.name}:${difficulty.name}"
        val current = variantStats[key] ?: DifficultyStats()
        val updated = current.record(result)
        val earnedXp = XpRules.forGame(variant, difficulty, result)
        return copy(
            xp = xp + earnedXp,
            variantStats = variantStats + (key to updated),
        )
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(rawJson: String): PlayerProgress {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val version = root["version"]?.jsonPrimitive?.intOrNull ?: 1
            if (version == 1) {
                val rawStats = root["stats"]?.jsonObject ?: JsonObject(emptyMap())
                val migratedVariantStats = buildMap {
                    for ((diffName, statElem) in rawStats) {
                        val diff = Difficulty.entries.firstOrNull { it.name == diffName } ?: continue
                        val stat = json.decodeFromJsonElement<DifficultyStats>(statElem)
                        put("${GameVariant.CLASSIC_3X3.name}:${diff.name}", stat)
                    }
                }
                val streak = root["streak"]?.let { json.decodeFromJsonElement<StreakState>(it) } ?: StreakState()
                val xp = root["xp"]?.jsonPrimitive?.intOrNull ?: 0
                return PlayerProgress(
                    version = 2,
                    xp = xp,
                    variantStats = migratedVariantStats,
                    streak = streak,
                )
            }
            return json.decodeFromJsonElement(root)
        }
    }
}
