package com.example.tictactoe.ads

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAdsManager = staticCompositionLocalOf<AdsManager> { NoOpAdsManager() }
