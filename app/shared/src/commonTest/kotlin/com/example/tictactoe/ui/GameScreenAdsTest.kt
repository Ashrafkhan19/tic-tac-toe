package com.example.tictactoe.ui

import com.example.tictactoe.ads.AdsManager
import com.example.tictactoe.domain.model.GameVariant
import com.example.tictactoe.domain.model.Mark
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.presentation.Intent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameScreenAdsTest {
    class TrackingAdsManager : AdsManager {
        var completedCalls = 0
        var interstitialCalls = 0
        override val isBannerEnabled = false

        @androidx.compose.runtime.Composable
        override fun BannerAd(modifier: androidx.compose.ui.Modifier) {}

        override fun onGameCompleted() {
            completedCalls++
        }

        override fun canShowInterstitial() = true

        override fun showInterstitialIfReady(onDismiss: () -> Unit) {
            interstitialCalls++
            onDismiss()
        }

        override fun loadInterstitial() {}
    }

    @Test
    fun adsManagerRespondsToGameLifecycle() {
        val tracking = TrackingAdsManager()
        tracking.onGameCompleted()
        assertEquals(1, tracking.completedCalls)

        var dismissed = false
        tracking.showInterstitialIfReady { dismissed = true }
        assertEquals(1, tracking.interstitialCalls)
        assertTrue(dismissed)
    }

    @Test
    fun newRoundInterceptedByInterstitial() {
        val tracking = TrackingAdsManager()
        var intentDispatched: Intent? = null
        val onIntent: (Intent) -> Unit = { intentDispatched = it }

        val handleIntentWithAds = createAdInterceptingIntentHandler(tracking, onIntent)

        handleIntentWithAds(Intent.NewRound)
        assertEquals(1, tracking.interstitialCalls)
        assertEquals(Intent.NewRound, intentDispatched)

        handleIntentWithAds(Intent.ResetScores)
        assertEquals(1, tracking.interstitialCalls)
        assertEquals(Intent.ResetScores, intentDispatched)
    }

    @Test
    fun computeCompletedOutcomeKeyReturnsNullForInProgress() {
        val key = computeCompletedOutcomeKey(
            variant = GameVariant.CLASSIC_3X3,
            xWins = 1,
            oWins = 0,
            draws = 0,
            outcome = Outcome.InProgress,
        )
        assertNull(key)
    }

    @Test
    fun computeCompletedOutcomeKeyReturnsKeyForWonAndDraw() {
        val wonKey = computeCompletedOutcomeKey(
            variant = GameVariant.CLASSIC_3X3,
            xWins = 1,
            oWins = 0,
            draws = 0,
            outcome = Outcome.Won(Mark.X, listOf(0, 1, 2)),
        )
        assertEquals("CLASSIC_3X3-1-0-0-Won(winner=X, line=[0, 1, 2])", wonKey)

        val drawKey = computeCompletedOutcomeKey(
            variant = GameVariant.GRID_4X4,
            xWins = 2,
            oWins = 1,
            draws = 3,
            outcome = Outcome.Draw,
        )
        assertEquals("GRID_4X4-2-1-3-Draw", drawKey)
    }

    @Test
    fun simulateGameCompletionDedupingAcrossRotation() {
        val tracking = TrackingAdsManager()
        var lastRecordedOutcome: String? = null

        fun dispatchOutcome(key: String?) {
            if (key != null) {
                if (lastRecordedOutcome != key) {
                    lastRecordedOutcome = key
                    tracking.onGameCompleted()
                }
            } else {
                lastRecordedOutcome = null
            }
        }

        // 1. Game won
        val game1Key = computeCompletedOutcomeKey(
            variant = GameVariant.CLASSIC_3X3,
            xWins = 1,
            oWins = 0,
            draws = 0,
            outcome = Outcome.Won(Mark.X, listOf(0, 1, 2)),
        )
        dispatchOutcome(game1Key)
        assertEquals(1, tracking.completedCalls)
        assertEquals(game1Key, lastRecordedOutcome)

        // 2. Device rotated during game over screen: same key dispatched with preserved lastRecordedOutcome
        dispatchOutcome(game1Key)
        assertEquals(1, tracking.completedCalls, "Rotation must not trigger a duplicate onGameCompleted")

        // 3. New round starts (InProgress)
        val inProgressKey = computeCompletedOutcomeKey(
            variant = GameVariant.CLASSIC_3X3,
            xWins = 1,
            oWins = 0,
            draws = 0,
            outcome = Outcome.InProgress,
        )
        dispatchOutcome(inProgressKey)
        assertEquals(1, tracking.completedCalls)
        assertNull(lastRecordedOutcome)

        // 4. Second game won
        val game2Key = computeCompletedOutcomeKey(
            variant = GameVariant.CLASSIC_3X3,
            xWins = 2,
            oWins = 0,
            draws = 0,
            outcome = Outcome.Won(Mark.X, listOf(3, 4, 5)),
        )
        dispatchOutcome(game2Key)
        assertEquals(2, tracking.completedCalls)
    }
}
