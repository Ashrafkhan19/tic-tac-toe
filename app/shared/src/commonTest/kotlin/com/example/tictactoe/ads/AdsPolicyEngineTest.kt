package com.example.tictactoe.ads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdsPolicyEngineTest {
    private var currentTime = 100_000L
    private val policy = AdsPolicyEngine(
        completedGamesThreshold = 3,
        cooldownDurationMs = 60_000L,
        currentTimeProvider = { currentTime }
    )

    @Test
    fun doesNotAllowInterstitialBeforeThreshold() {
        assertFalse(policy.canShowInterstitial())
        policy.recordGameCompleted()
        assertEquals(1, policy.completedGamesCount)
        assertFalse(policy.canShowInterstitial())
        policy.recordGameCompleted()
        assertEquals(2, policy.completedGamesCount)
        assertFalse(policy.canShowInterstitial())
    }

    @Test
    fun allowsInterstitialWhenThresholdReachedAndCooldownSatisfied() {
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        assertEquals(3, policy.completedGamesCount)
        assertTrue(policy.canShowInterstitial())
    }

    @Test
    fun resetsCounterAndAppliesCooldownOnAdShown() {
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        assertTrue(policy.canShowInterstitial())

        policy.markAdShown()
        assertEquals(0, policy.completedGamesCount)
        assertEquals(100_000L, policy.lastAdShownTimestampMs)
        assertFalse(policy.canShowInterstitial())

        // Complete 3 more games immediately (only 10 seconds later)
        currentTime += 10_000L
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        assertEquals(3, policy.completedGamesCount)
        // Cooldown has not elapsed (10s < 60s)
        assertFalse(policy.canShowInterstitial())

        // Advance past cooldown (60s later)
        currentTime += 50_000L
        assertTrue(policy.canShowInterstitial())
    }

    @Test
    fun allowsInterstitialAfterFocusWorkoutAndCooldown() {
        assertFalse(policy.canShowInterstitial())
        policy.recordFocusWorkoutCompleted()
        assertEquals(1, policy.completedWorkoutsCount)
        assertTrue(policy.canShowInterstitial())

        policy.markAdShown()
        assertEquals(0, policy.completedWorkoutsCount)
        assertFalse(policy.canShowInterstitial())
    }

    @Test
    fun blocksInterstitialDuringInitialGrace() {
        var now = 1_000L
        val graced = AdsPolicyEngine(completedGamesThreshold = 1, cooldownDurationMs = 0L, initialGraceMs = 120_000L, currentTimeProvider = { now })
        graced.recordGameCompleted()
        assertFalse(graced.canShowInterstitial())
        now += 120_000L
        assertTrue(graced.canShowInterstitial())
    }

    @Test
    fun rewardedShownStartsCooldownButKeepsProgress() {
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.markRewardedShown()
        assertEquals(3, policy.completedGamesCount)
        assertFalse(policy.canShowInterstitial())
        currentTime += 60_000L
        assertTrue(policy.canShowInterstitial())
    }
}
