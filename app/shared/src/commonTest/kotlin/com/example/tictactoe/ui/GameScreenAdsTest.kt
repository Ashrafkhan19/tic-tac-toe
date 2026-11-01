package com.example.tictactoe.ui

import com.example.tictactoe.ads.AdsManager
import com.example.tictactoe.presentation.Intent
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
