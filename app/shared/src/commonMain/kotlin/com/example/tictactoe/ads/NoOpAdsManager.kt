package com.example.tictactoe.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Default no-op ad manager for non-ad platforms, tests, and previews.
 */
class NoOpAdsManager : AdsManager {
    override val isBannerEnabled: Boolean = false

    @Composable
    override fun BannerAd(modifier: Modifier) {
        // No-op for non-ad environments
    }

    override fun onGameCompleted() {}
    override fun canShowInterstitial(): Boolean = false
    override fun showInterstitialIfReady(onDismiss: () -> Unit) {
        onDismiss()
    }
    override fun loadInterstitial() {}
}
