package com.example.tictactoe.ads

import kotlin.test.Test
import kotlin.test.assertEquals

class AdConfigTest {

    @Test
    fun testProductionIdsConfigured() {
        assertEquals("ca-app-pub-3749655808000338~9944413906", AdConfig.PRODUCTION_APP_ID)
        assertEquals("ca-app-pub-3749655808000338/7751871546", AdConfig.PRODUCTION_BANNER_AD_UNIT_ID)
        assertEquals("ca-app-pub-3749655808000338/8653889676", AdConfig.PRODUCTION_INTERSTITIAL_AD_UNIT_ID)
    }

    @Test
    fun testBannerAdUnitIdResolution() {
        assertEquals(AdConfig.TEST_BANNER_AD_UNIT_ID, AdConfig.getBannerAdUnitId(isDebug = true))
        assertEquals(AdConfig.PRODUCTION_BANNER_AD_UNIT_ID, AdConfig.getBannerAdUnitId(isDebug = false))
    }

    @Test
    fun testInterstitialAdUnitIdResolution() {
        assertEquals(AdConfig.TEST_INTERSTITIAL_AD_UNIT_ID, AdConfig.getInterstitialAdUnitId(isDebug = true))
        assertEquals(AdConfig.PRODUCTION_INTERSTITIAL_AD_UNIT_ID, AdConfig.getInterstitialAdUnitId(isDebug = false))
    }
}
