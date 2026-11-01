package com.example.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.tictactoe.ads.AndroidAdsManager
import com.example.tictactoe.di.AppContainer
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private var sharedAdsManager: AndroidAdsManager? = null
        private var sharedAppContainer: AppContainer? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }

        val ads = (sharedAdsManager ?: AndroidAdsManager()).apply {
            setActivity(this@MainActivity)
        }
        sharedAdsManager = ads

        val container = sharedAppContainer ?: AppContainer(adsManager = ads).also {
            sharedAppContainer = it
        }

        setContent {
            App(container = container)
        }
    }

    override fun onDestroy() {
        sharedAdsManager?.clearActivity(this)
        super.onDestroy()
    }
}
