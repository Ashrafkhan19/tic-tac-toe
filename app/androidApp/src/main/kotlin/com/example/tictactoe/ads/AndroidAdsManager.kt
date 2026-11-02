package com.example.tictactoe.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    private val currentTimeProvider: () -> Long = { SystemClock.elapsedRealtime() },
) : AdsManager {
    constructor(activity: Activity) : this({ activity })

    override val isBannerEnabled: Boolean = true

    private val policy = AdsPolicyEngine(
        completedGamesThreshold = 3,
        cooldownDurationMs = 90_000L,
        initialGraceMs = 120_000L,
        currentTimeProvider = currentTimeProvider,
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoadedAtMs: Long = 0L
    private var isLoadingInterstitial: Boolean = false
    private var interstitialRetries: Int = 0

    // State-backed so the Gym "watch to earn" card appears as soon as the ad finishes loading.
    private var rewardedAd: RewardedAd? by mutableStateOf(null)
    private var rewardedLoadedAtMs: Long = 0L
    private var isLoadingRewarded: Boolean = false
    private var rewardedRetries: Int = 0

    // AdMob invalidates cached full-screen ads after about an hour; showing a stale one fails.
    private fun isFresh(loadedAtMs: Long): Boolean = currentTimeProvider() - loadedAtMs < AD_MAX_AGE_MS

    private fun retryDelayMs(attempt: Int): Long = RETRY_BASE_DELAY_MS shl (attempt - 1)

    // Ad loads touch the Ads SDK (class init + Play Services binder calls), so they must not run
    // before MobileAds.initialize completes or they can block the main thread during startup.
    private var isSdkInitialized: Boolean by mutableStateOf(false)

    fun onSdkInitialized() {
        isSdkInitialized = true
        loadInterstitial()
        loadRewarded()
    }

    fun setActivity(activity: Activity?) {
        this.activityProvider = { activity }
        if (activity != null && isSdkInitialized) {
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
        if (!isSdkInitialized) return
        val context = LocalContext.current
        val adSize = remember(context) {
            val metrics = context.resources.displayMetrics
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, (metrics.widthPixels / metrics.density).toInt())
        }
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(adSize)
                    adUnitId = AdConfig.getBannerAdUnitId()
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
        return policy.canShowInterstitial() && hasFreshInterstitial() && isActivityValid
    }

    private fun hasFreshInterstitial(): Boolean {
        if (interstitialAd != null && !isFresh(interstitialLoadedAtMs)) {
            interstitialAd = null
            loadInterstitial()
        }
        return interstitialAd != null
    }

    override fun showInterstitialIfReady(onDismiss: () -> Unit) {
        val activity = activityProvider()
        val ad = if (hasFreshInterstitial()) interstitialAd else null
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
        if (!isSdkInitialized) return
        val activity = activityProvider() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (isLoadingInterstitial || interstitialAd != null) return

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            AdConfig.getInterstitialAdUnitId(),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialLoadedAtMs = currentTimeProvider()
                    interstitialRetries = 0
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    if (interstitialRetries < MAX_LOAD_RETRIES) {
                        interstitialRetries++
                        mainHandler.postDelayed({ loadInterstitial() }, retryDelayMs(interstitialRetries))
                    }
                }
            }
        )
    }

    override fun canShowRewarded(): Boolean {
        val activity = activityProvider()
        val isActivityValid = activity != null && !activity.isFinishing && !activity.isDestroyed
        if (rewardedAd != null && !isFresh(rewardedLoadedAtMs)) {
            rewardedAd = null
            loadRewarded()
        }
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
                        // The user just sat through an ad; don't chase it with an interstitial.
                        policy.markRewardedShown()
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
        if (!isSdkInitialized) return
        val activity = activityProvider() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (isLoadingRewarded || rewardedAd != null) return
        val adUnitId = AdConfig.getRewardedAdUnitId() ?: return

        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedLoadedAtMs = currentTimeProvider()
                    rewardedRetries = 0
                    isLoadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    if (rewardedRetries < MAX_LOAD_RETRIES) {
                        rewardedRetries++
                        mainHandler.postDelayed({ loadRewarded() }, retryDelayMs(rewardedRetries))
                    }
                }
            }
        )
    }

    private companion object {
        const val AD_MAX_AGE_MS = 55 * 60_000L
        const val MAX_LOAD_RETRIES = 3
        const val RETRY_BASE_DELAY_MS = 5_000L
    }
}
