package com.example.tictactoe.domain.focus

enum class FocusSessionType {
    URGE_SURF,
    STROOP_DRILL,
    N_BACK_DRILL,
    DEEP_READING,
    FOCUS_TIMER,
    STRATEGY_MATCH
}

data class FocusSessionResult(
    val id: String,
    val type: FocusSessionType,
    val durationSeconds: Int,
    val scorePercent: Int? = null,
    val distractionCount: Int = 0,
    val timestampMillis: Long = 0L
)

data class DailyCircuitState(
    val dateIso: String,
    val urgeWarmupCompleted: Boolean = false,
    val neuroDrillCompleted: Boolean = false,
    val deepActivityCompleted: Boolean = false,
    val sleepLogged: Boolean = false,
    val movementLogged: Boolean = false
) {
    val isComplete: Boolean get() = urgeWarmupCompleted && neuroDrillCompleted && deepActivityCompleted
}

object FocusXpRules {
    fun xpForUrgeSurfed(): Int = 20
    fun xpForDeepReading(scorePercent: Int): Int = if (scorePercent >= 66) 50 else 10
    fun xpForFocusSession(durationMinutes: Int, distractionCount: Int): Int {
        val base = durationMinutes.coerceAtLeast(1)
        val penalty = (distractionCount * 2).coerceAtMost(base / 2)
        return (base - penalty).coerceAtLeast(5)
    }
    fun xpForDailyFoundation(): Int = 15
}

