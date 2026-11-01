package com.example.tictactoe.ads

import com.example.tictactoe.di.AppContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppContainerAdsTest {
    class FakeAdsManager : AdsManager {
        var completedGames = 0
        var interstitialShown = 0
        override val isBannerEnabled = true

        @androidx.compose.runtime.Composable
        override fun BannerAd(modifier: androidx.compose.ui.Modifier) {}

        override fun onGameCompleted() { completedGames++ }
        override fun canShowInterstitial() = true
        override fun showInterstitialIfReady(onDismiss: () -> Unit) {
            interstitialShown++
            onDismiss()
        }
        override fun loadInterstitial() {}
    }

    @Test
    fun defaultContainerUsesNoOpAdsManager() {
        val container = AppContainer()
        assertTrue(container.adsManager is NoOpAdsManager)
    }

    @Test
    fun customAdsManagerIsInjectedSuccessfully() {
        val fakeAds = FakeAdsManager()
        val container = AppContainer(adsManager = fakeAds)
        assertEquals(fakeAds, container.adsManager)
    }

    @Test
    fun noOpAdsManagerContract() {
        val noOp = NoOpAdsManager()
        assertFalse(noOp.isBannerEnabled)
        assertFalse(noOp.canShowInterstitial())
        var dismissed = false
        noOp.showInterstitialIfReady { dismissed = true }
        assertTrue(dismissed)
        noOp.onGameCompleted()
        noOp.loadInterstitial()
    }
}
