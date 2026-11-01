# Google Mobile Ads Support Design Spec

## Intent & Overview
This specification details the architecture and implementation plan for integrating Google Mobile Ads (AdMob) into the TicTacToe game.

The monetization strategy introduces:
1. **Persistent Banner Ad**: Non-intrusive banner anchored at the bottom of the screen with proper window insets handling.
2. **Intermittent Interstitial Ad**: Full-screen interstitial shown during natural game breaks (on "Play Again" / "Next Round" action) once threshold criteria are met:
   - At least 3 games completed since the previous interstitial.
   - At least 60 seconds of elapsed cooldown since the previous interstitial.
   - Never interrupts active moves or celebration/confetti animations.
3. **Multiplatform Decoupling**: Full native integration on Android via Google Play Services Ads (`play-services-ads`), paired with clean no-op abstractions for Desktop, iOS, and Compose `@Preview` / unit tests.
4. **Safety & Testability**: Default to Google official sample App ID and Test Ad Unit IDs for safe development and review, with clean override points for production IDs.

---

## Architecture & Multiplatform Contracts

### 1. `AdsManager` & Policy Engine (`app:shared` - `commonMain`)
The monetization logic is encapsulated behind clean interfaces decoupled from Android framework types:

```kotlin
package com.example.tictactoe.ads

/**
 * Manages ad lifecycle, frequency capping, and ad presentation.
 */
interface AdsManager {
    val isBannerEnabled: Boolean
    fun onGameCompleted()
    fun canShowInterstitial(): Boolean
    fun showInterstitialIfReady(onDismiss: () -> Unit)
    fun loadInterstitial()
}
```

```kotlin
package com.example.tictactoe.ads

/**
 * Pure policy engine determining whether an interstitial ad should be triggered.
 * Kept independent of platform time APIs for deterministic unit testing.
 */
class AdsPolicyEngine(
    private val completedGamesThreshold: Int = 3,
    private val cooldownDurationMs: Long = 60_000L,
    private val currentTimeProvider: () -> Long = { SystemTodayProvider.currentTimeMillis() },
) {
    var completedGamesCount: Int = 0
        private set
    var lastAdShownTimestampMs: Long = 0L
        private set

    fun recordGameCompleted() {
        completedGamesCount++
    }

    fun canShowInterstitial(): Boolean {
        val now = currentTimeProvider()
        val cooldownPassed = (now - lastAdShownTimestampMs) >= cooldownDurationMs
        return completedGamesCount >= completedGamesThreshold && cooldownPassed
    }

    fun markAdShown() {
        completedGamesCount = 0
        lastAdShownTimestampMs = currentTimeProvider()
    }
}
```

### 2. No-Op Implementation (`commonMain`)
Used on Desktop, iOS (initial phase), unit tests, and Compose previews:
```kotlin
package com.example.tictactoe.ads

class NoOpAdsManager : AdsManager {
    override val isBannerEnabled: Boolean = false
    override fun onGameCompleted() {}
    override fun canShowInterstitial(): Boolean = false
    override fun showInterstitialIfReady(onDismiss: () -> Unit) {
        onDismiss()
    }
    override fun loadInterstitial() {}
}
```

### 3. Composition & Dependency Injection (`AppContainer`)
`AppContainer` owns the `AdsManager` instance:
```kotlin
class AppContainer(
    ...,
    val adsManager: AdsManager = NoOpAdsManager(),
)
```
In Compose, `LocalAdsManager` provides the instance down the tree:
```kotlin
val LocalAdsManager = staticCompositionLocalOf<AdsManager> { NoOpAdsManager() }
```

---

## Android Implementation (`app:androidApp` & `app:shared`)

### 1. Dependencies & Manifest Configuration
- **Version Catalog** (`gradle/libs.versions.toml`):
  ```toml
  [versions]
  playServicesAds = "23.6.0"

  [libraries]
  play-services-ads = { module = "com.google.android.gms:play-services-ads", version.ref = "playServicesAds" }
  ```
- **Android Manifest** (`app/androidApp/src/main/AndroidManifest.xml`):
  Include sample Google Mobile Ads Application ID (or resource string):
  ```xml
  <manifest ...>
      <application ...>
          <!-- Sample AdMob App ID for development and testing -->
          <meta-data
              android:name="com.google.android.gms.ads.APPLICATION_ID"
              android:value="ca-app-pub-3940256099942544~3347511713"/>
          ...
      </application>
  </manifest>
  ```

### 2. Ad Unit Constants
Google's standard test Ad Unit IDs:
- **Test Banner ID**: `ca-app-pub-3940256099942544/6300978111`
- **Test Interstitial ID**: `ca-app-pub-3940256099942544/1033173712`

### 3. `AndroidAdsManager` (`androidMain`)
Implements `AdsManager` using Google Mobile Ads SDK:
- **Initialization**: MobileAds initialized in background dispatcher upon Activity start.
- **Interstitial Cache**:
  - Maintains `var interstitialAd: InterstitialAd? = null`.
  - Automatically invokes `loadInterstitial()` on launch and immediately after any interstitial is closed or fails to show.
  - Attaches `FullScreenContentCallback` to handle `onAdDismissedFullScreenContent`, `onAdFailedToShowFullScreenContent`, and `onAdShowedFullScreenContent`.
  - On dismiss or failure, dispatches `onDismiss()` to caller immediately so gameplay never hangs.
- **Activity Context**: Weakly referenced or passed as `() -> Activity?` to avoid Activity memory leaks.

### 4. `BannerAd` Composable
- `expect @Composable fun BannerAd(modifier: Modifier = Modifier)` in `commonMain`.
- `actual @Composable fun BannerAd(modifier: Modifier = Modifier)`:
  - **`androidMain`**:
    Renders an `AndroidView` creating an `AdView(context)`:
    ```kotlin
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdConfig.bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { /* update if needed */ },
        onRelease = { it.destroy() }
    )
    ```
  - **`jvmMain` / `iosMain`**:
    ```kotlin
    @Composable
    actual fun BannerAd(modifier: Modifier) {
        // No-op placeholder for Desktop / iOS / Previews
    }
    ```

---

## UI Layout & Game Flow Integration

### 1. `BannerAd` Positioning in `GameScreen`
- In `CompactLayout`:
  - Positioned at the bottom of the screen, below the scrolling board/controls container.
  - Accommodates navigation bar / safe drawing insets.
  - Only visible if `adsManager.isBannerEnabled`.
- In `WideLayout`:
  - Positioned below the right-hand action card or centered along the bottom bar.

### 2. Interstitial Ad Trigger Point
- Follows Google Play "Better Ads" guidelines (no surprise ads during action or celebratory states):
- When a game finishes (`Outcome.Won` or `Outcome.Draw`), the user sees the final board state, celebration confetti, and status.
- When the user taps **"Play Again"** or **"Start Next Round"**:
  1. The UI invokes `adsManager.showInterstitialIfReady(onDismiss = { onIntent(Intent.StartNextRound) })`.
  2. If an interstitial is ready and policy criteria are satisfied, the ad is displayed.
  3. When the user dismisses the ad (or if no ad is ready / policy criteria not met), `onDismiss` fires immediately, initiating the next round.
- When resetting scores or switching game modes / variants, `onGameCompleted()` is NOT incremented, preventing unexpected ad triggers during menu navigation.

---

## Testing & Verification Plan

### 1. Unit Tests (`commonTest`)
- **`AdsPolicyEngineTest`**:
  - Verify `completedGamesCount` increments on `recordGameCompleted()`.
  - Verify `canShowInterstitial()` returns `false` for 0, 1, and 2 completed games.
  - Verify `canShowInterstitial()` returns `true` on the 3rd completed game when cooldown has elapsed.
  - Verify `canShowInterstitial()` returns `false` if 3 games are completed but 60s cooldown has not elapsed.
  - Verify `markAdShown()` resets counter to 0 and updates `lastAdShownTimestampMs`.
- **`AppContainerTest`**:
  - Verify `NoOpAdsManager` handles all calls silently without null pointers or exceptions.

### 2. Integration & Multiplatform Build Verification
- Execute `./gradlew :core:test :app:shared:allTests` to ensure shared multiplatform tests pass cleanly without regression.
- Execute `./gradlew :app:androidApp:assembleDebug` to verify Android build compilation, Manifest merge, AdMob SDK linking, and ProGuard / resource configuration.
- Execute Compose previews / Desktop run to ensure non-Android targets build and execute without errors.
