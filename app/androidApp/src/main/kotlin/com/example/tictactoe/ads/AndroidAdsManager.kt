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
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

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
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded: Boolean = false

    init {
        loadInterstitial()
        loadRewarded()
    }

    fun setActivity(activity: Activity?) {
        this.activityProvider = { activity }
        if (activity != null) {
            if (interstitialAd == null) loadInterstitial()
            if (rewardedAd == null) loadRewarded()
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

    override fun onFocusWorkoutCompleted() {
        policy.recordFocusWorkoutCompleted()
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

    override fun canShowRewarded(): Boolean {
        val activity = activityProvider()
        val isActivityValid = activity != null && !activity.isFinishing && !activity.isDestroyed
        return rewardedAd != null && isActivityValid
    }

    override fun showRewardedIfReady(onRewardEarned: (rewardAmount: Int) -> Unit, onDismiss: () -> Unit) {
        val activity = activityProvider()
        val ad = rewardedAd
        if (activity != null && !activity.isFinishing && !activity.isDestroyed && ad != null) {
            var earnedReward = false
            var rewardAmount = 1
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded()
                    if (earnedReward) {
                        onRewardEarned(rewardAmount)
                    }
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewarded()
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    rewardedAd = null
                }
            }
            ad.show(activity) { item: RewardItem ->
                earnedReward = true
                rewardAmount = item.amount
            }
        } else {
            if (rewardedAd == null) {
                loadRewarded()
            }
            onDismiss()
        }
    }

    override fun loadRewarded() {
        val activity = activityProvider() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (isLoadingRewarded || rewardedAd != null) return

        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            AdConfig.TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                }
            }
        )
    }
}
