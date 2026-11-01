package com.example.tictactoe.ads

import android.app.Activity
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AndroidAdsManager(
    private var activityProvider: () -> Activity? = { null },
    currentTimeProvider: () -> Long = { SystemClock.elapsedRealtime() },
) : AdsManager {
    constructor(activity: Activity) : this({ activity })

    override val isBannerEnabled: Boolean = true

    private val policy = AdsPolicyEngine(
        completedGamesThreshold = 3,
        cooldownDurationMs = 60_000L,
        currentTimeProvider = currentTimeProvider,
    )

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial: Boolean = false

    init {
        loadInterstitial()
    }

    fun setActivity(activity: Activity?) {
        this.activityProvider = { activity }
        if (activity != null && interstitialAd == null) {
            loadInterstitial()
        }
    }

    fun clearActivity(activity: Activity? = null) {
        if (activity == null || activityProvider() === activity) {
            this.activityProvider = { null }
        }
    }

    @Composable
    override fun BannerAd(modifier: Modifier) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdConfig.TEST_BANNER_AD_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { adView ->
                adView.destroy()
            }
        )
    }

    override fun onGameCompleted() {
        policy.recordGameCompleted()
    }

    override fun canShowInterstitial(): Boolean {
        val activity = activityProvider()
        val isActivityValid = activity != null && !activity.isFinishing && !activity.isDestroyed
        return policy.canShowInterstitial() && interstitialAd != null && isActivityValid
    }

    override fun showInterstitialIfReady(onDismiss: () -> Unit) {
        val activity = activityProvider()
        val ad = interstitialAd
        if (activity != null && !activity.isFinishing && !activity.isDestroyed && ad != null && policy.canShowInterstitial()) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    policy.markAdShown()
                    loadInterstitial()
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitial()
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                }
            }
            ad.show(activity)
        } else {
            if (interstitialAd == null) {
                loadInterstitial()
            }
            onDismiss()
        }
    }

    override fun loadInterstitial() {
        val activity = activityProvider() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (isLoadingInterstitial || interstitialAd != null) return

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            AdConfig.TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }
}
