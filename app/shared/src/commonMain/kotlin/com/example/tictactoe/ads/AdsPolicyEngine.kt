package com.example.tictactoe.ads

/**
 * Pure policy engine determining whether an interstitial ad is eligible to trigger.
 */
class AdsPolicyEngine(
    val completedGamesThreshold: Int = 3,
    val cooldownDurationMs: Long = 60_000L,
    /** No interstitial until the user has been in the app this long, so the first minutes stay ad-free. */
    val initialGraceMs: Long = 0L,
    private val currentTimeProvider: () -> Long,
) {
    private val startTimestampMs: Long = currentTimeProvider()

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
        if (now - startTimestampMs < initialGraceMs) return false
        return lastAdShownTimestampMs == 0L || (now - lastAdShownTimestampMs) >= cooldownDurationMs
    }

    /** A rewarded ad the user chose to watch counts toward the cooldown, but must not wipe progress toward the next interstitial. */
    fun markRewardedShown() {
        lastAdShownTimestampMs = currentTimeProvider()
    }

    fun markAdShown() {
        completedGamesCount = 0
        completedWorkoutsCount = 0
        lastAdShownTimestampMs = currentTimeProvider()
    }
}
