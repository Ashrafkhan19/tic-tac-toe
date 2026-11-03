package com.example.tictactoe.analytics

/** Default tracker for platforms without analytics, tests, and previews. */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any>) {}
    override fun logScreenView(screenName: String) {}
}
