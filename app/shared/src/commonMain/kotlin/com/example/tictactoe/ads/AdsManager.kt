package com.example.tictactoe.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-independent contract for ad management and presentation.
 */
interface AdsManager {
    val isBannerEnabled: Boolean

    @Composable
    fun BannerAd(modifier: Modifier = Modifier)

    fun onGameCompleted()
    fun canShowInterstitial(): Boolean
    fun showInterstitialIfReady(onDismiss: () -> Unit)
    fun loadInterstitial()
}
