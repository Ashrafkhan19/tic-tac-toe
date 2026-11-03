package com.example.tictactoe.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class AndroidAnalyticsTracker(context: Context) : AnalyticsTracker {
    private val analytics = FirebaseAnalytics.getInstance(context.applicationContext)

    override fun logEvent(name: String, params: Map<String, Any>) {
        runCatching { analytics.logEvent(name, params.toBundle()) }
    }

    override fun logScreenView(screenName: String) {
        runCatching {
            analytics.logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
                },
            )
            // Breadcrumb so crash reports show which screen the user was on.
            FirebaseCrashlytics.getInstance().setCustomKey("screen", screenName)
        }
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().also { bundle ->
        forEach { (key, value) ->
            when (value) {
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
    }
}
