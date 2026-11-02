package com.example.tictactoe.ads

import com.example.tictactoe.BuildConfig

object AdConfig {
    // Production IDs
    const val PRODUCTION_APP_ID = "ca-app-pub-3749655808000338~9944413906"
    const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-3749655808000338/7751871546"
    const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3749655808000338/8653889676"

    // Create a Rewarded unit in AdMob (app 9944413906) and paste its ID here. Until then rewarded is
    // disabled in release builds; the Google test unit must never serve on production traffic.
    const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-3749655808000338/2251279223"

    // Google Test IDs (Used during development/debug to strictly follow AdMob policy on invalid traffic)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Backward-compatibility alias
    const val SAMPLE_APP_ID = TEST_APP_ID

    fun getBannerAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String {
        return if (isDebug) TEST_BANNER_AD_UNIT_ID else PRODUCTION_BANNER_AD_UNIT_ID
    }

    fun getInterstitialAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String {
        return if (isDebug) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID
    }

    fun getRewardedAdUnitId(isDebug: Boolean = BuildConfig.DEBUG): String? {
        return if (isDebug) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID.ifBlank { null }
    }
}
