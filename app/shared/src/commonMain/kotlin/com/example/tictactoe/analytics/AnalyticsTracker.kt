package com.example.tictactoe.analytics

/**
 * Platform-independent contract for product analytics. Implementations must never throw.
 */
interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun logScreenView(screenName: String)
}

/** Event and parameter names, kept in one place so dashboards stay consistent. */
object AnalyticsEvents {
    const val WORKOUT_COMPLETE = "workout_complete"
    const val GAME_COMPLETE = "game_complete"
    const val FOUNDATION_LOGGED = "foundation_logged"
    const val REWARDED_XP_CLAIMED = "rewarded_xp_claimed"
    const val NEW_PERSONAL_BEST = "new_personal_best"

    const val PARAM_TYPE = "type"
    const val PARAM_XP = "xp"
    const val PARAM_SCORE = "score"
    const val PARAM_DURATION_MIN = "duration_min"
    const val PARAM_DISTRACTIONS = "distractions"
    const val PARAM_VARIANT = "variant"
    const val PARAM_MODE = "mode"
    const val PARAM_OUTCOME = "outcome"
    const val PARAM_METRIC = "metric"
    const val PARAM_VALUE = "value"
}
