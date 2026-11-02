package com.example.tictactoe.ads

/**
 * Pure policy engine determining whether an interstitial ad is eligible to trigger.
 */
class AdsPolicyEngine(
    val completedGamesThreshold: Int = 3,
    val cooldownDurationMs: Long = 60_000L,
    private val currentTimeProvider: () -> Long,
) {
    var completedGamesCount: Int = 0
        private set
    var completedWorkoutsCount: Int = 0
        private set
    var lastAdShownTimestampMs: Long = 0L
        private set

    fun recordGameCompleted() {
        completedGamesCount++
    }

    fun recordFocusWorkoutCompleted() {
        completedWorkoutsCount++
    }

    fun canShowInterstitial(): Boolean {
        val eligible = completedGamesCount >= completedGamesThreshold || completedWorkoutsCount >= 1
        if (!eligible) return false
        val now = currentTimeProvider()
        return (now - lastAdShownTimestampMs) >= cooldownDurationMs
    }

    fun markAdShown() {
        completedGamesCount = 0
        completedWorkoutsCount = 0
        lastAdShownTimestampMs = currentTimeProvider()
    }
}
