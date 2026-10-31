package com.example.tictactoe.data

import com.example.tictactoe.domain.progress.PlayerProgress
import com.example.tictactoe.domain.progress.ProgressRepository
import com.russhwolf.settings.Settings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Persists [PlayerProgress] as a single JSON string. Unreadable data falls back to defaults. */
class SettingsProgressRepository(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ProgressRepository {

    override fun load(): PlayerProgress {
        val raw = settings.getStringOrNull(KEY) ?: return PlayerProgress()
        return try {
            json.decodeFromString(PlayerProgress.serializer(), raw)
        } catch (_: SerializationException) {
            PlayerProgress()
        } catch (_: IllegalArgumentException) {
            PlayerProgress()
        }
    }

    override fun save(progress: PlayerProgress) {
        settings.putString(KEY, json.encodeToString(PlayerProgress.serializer(), progress))
    }

    private companion object {
        const val KEY = "player_progress_v1"
    }
}
