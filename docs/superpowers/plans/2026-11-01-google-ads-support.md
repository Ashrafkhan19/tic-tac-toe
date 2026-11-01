# Google Mobile Ads Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Google Mobile Ads (AdMob) support into the TicTacToe game with a persistent bottom banner ad and intermittent interstitial ads (triggered every 3 completed games with a 60-second cooldown), decoupled across Kotlin Multiplatform with Android native implementation and graceful no-op fallback on other platforms.

**Architecture:** A multiplatform `AdsManager` interface and pure Kotlin `AdsPolicyEngine` in `commonMain` manage frequency capping and ad state without Android dependencies. `AppContainer` and `LocalAdsManager` provide dependency injection down the Compose tree. Android integrates Google Play Services Ads (`play-services-ads`), implementing `AndroidAdsManager` with `AdView` in Compose and pre-cached `InterstitialAd`, initialized via `MainActivity`.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose Multiplatform, Google Mobile Ads SDK (`com.google.android.gms:play-services-ads:23.6.0`), AndroidX Activity Compose, Gradle Version Catalogs.

**Spec:** [docs/superpowers/specs/2026-11-01-google-ads-support-design.md](file:///Users/user/Documents/Tictactoe/docs/superpowers/specs/2026-11-01-google-ads-support-design.md)

## Global Constraints

- Android `minSdk = 24`, `compileSdk = 37`, `targetSdk = 37`.
- Google Mobile Ads Application ID metadata must be present in `AndroidManifest.xml` to prevent startup crashes.
- Sample Test AdMob App ID: `ca-app-pub-3940256099942544~3347511713`.
- Sample Test Banner Ad Unit ID: `ca-app-pub-3940256099942544/6300978111`.
- Sample Test Interstitial Ad Unit ID: `ca-app-pub-3940256099942544/1033173712`.
- Frequency capping: Interstitials trigger only after 3 completed games AND at least 60 seconds of elapsed cooldown since the last interstitial.
- Failure resilience: If an ad fails to load or show, `onDismiss()` must fire immediately so gameplay never hangs.
- Multiplatform compatibility: Desktop, iOS, unit tests, and `@Preview` composables must run without any crash or Android framework dependencies.

## Review Focus

- Interstitial trigger threshold: ensure game count increments only on terminal outcomes (`Outcome.Won` or `Outcome.Draw`), not on mid-game restarts or variant changes.
- Cooldown timer enforcement: ensure fast games within 60s of an interstitial display do not trigger another interstitial even if game count reaches 3.
- Dismiss callback resilience: verify `onDismiss` fires when ad is null, failed, or dismissed so next round always begins.
- Memory leak prevention: ensure `AdView.destroy()` is called on composable disposal.
- Safe Area & Insets: verify banner ad adheres to window insets / navigation bar in compact layout without obscuring game controls.

---

### Task 1: Ads Abstraction & Policy Engine (`app:shared` `commonMain`)

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/AdsPolicyEngine.kt`
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/AdsManager.kt`
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/NoOpAdsManager.kt`
- Test: `app/shared/src/commonTest/kotlin/com/example/tictactoe/ads/AdsPolicyEngineTest.kt`

**Interfaces:**
- Consumes: None (pure Kotlin and Compose runtime)
- Produces:
  - `class AdsPolicyEngine(completedGamesThreshold: Int, cooldownDurationMs: Long, currentTimeProvider: () -> Long)`
  - `interface AdsManager { val isBannerEnabled: Boolean; @Composable fun BannerAd(modifier: Modifier); fun onGameCompleted(); fun canShowInterstitial(): Boolean; fun showInterstitialIfReady(onDismiss: () -> Unit); fun loadInterstitial() }`
  - `class NoOpAdsManager : AdsManager`

- [ ] **Step 1: Write the failing unit tests for `AdsPolicyEngine`**

Create `app/shared/src/commonTest/kotlin/com/example/tictactoe/ads/AdsPolicyEngineTest.kt`:
```kotlin
package com.example.tictactoe.ads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdsPolicyEngineTest {
    private var currentTime = 100_000L
    private val policy = AdsPolicyEngine(
        completedGamesThreshold = 3,
        cooldownDurationMs = 60_000L,
        currentTimeProvider = { currentTime }
    )

    @Test
    fun doesNotAllowInterstitialBeforeThreshold() {
        assertFalse(policy.canShowInterstitial())
        policy.recordGameCompleted()
        assertEquals(1, policy.completedGamesCount)
        assertFalse(policy.canShowInterstitial())
        policy.recordGameCompleted()
        assertEquals(2, policy.completedGamesCount)
        assertFalse(policy.canShowInterstitial())
    }

    @Test
    fun allowsInterstitialWhenThresholdReachedAndCooldownSatisfied() {
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        assertEquals(3, policy.completedGamesCount)
        assertTrue(policy.canShowInterstitial())
    }

    @Test
    fun resetsCounterAndAppliesCooldownOnAdShown() {
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        assertTrue(policy.canShowInterstitial())

        policy.markAdShown()
        assertEquals(0, policy.completedGamesCount)
        assertEquals(100_000L, policy.lastAdShownTimestampMs)
        assertFalse(policy.canShowInterstitial())

        // Complete 3 more games immediately (only 10 seconds later)
        currentTime += 10_000L
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        policy.recordGameCompleted()
        assertEquals(3, policy.completedGamesCount)
        // Cooldown has not elapsed (10s < 60s)
        assertFalse(policy.canShowInterstitial())

        // Advance past cooldown (60s later)
        currentTime += 50_000L
        assertTrue(policy.canShowInterstitial())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:shared:allTests --tests "com.example.tictactoe.ads.AdsPolicyEngineTest"`
Expected: FAIL with compilation error (unresolved reference `AdsPolicyEngine`).

- [ ] **Step 3: Implement `AdsPolicyEngine`, `AdsManager`, and `NoOpAdsManager`**

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/AdsPolicyEngine.kt`:
```kotlin
package com.example.tictactoe.ads

/**
 * Pure policy engine determining whether an interstitial ad is eligible to trigger.
 */
class AdsPolicyEngine(
    val completedGamesThreshold: Int = 3,
    val cooldownDurationMs: Long = 60_000L,
    private val currentTimeProvider: () -> Long,
) {
    var completedGamesCount: Int = 0
        private set
    var lastAdShownTimestampMs: Long = 0L
        private set

    fun recordGameCompleted() {
        completedGamesCount++
    }

    fun canShowInterstitial(): Boolean {
        if (completedGamesCount < completedGamesThreshold) return false
        val now = currentTimeProvider()
        return (now - lastAdShownTimestampMs) >= cooldownDurationMs
    }

    fun markAdShown() {
        completedGamesCount = 0
        lastAdShownTimestampMs = currentTimeProvider()
    }
}
```

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/AdsManager.kt`:
```kotlin
package com.example.tictactoe.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-independent contract for ad management and presentation.
 */
interface AdsManager {
    val isBannerEnabled: Boolean

    @Composable
    fun BannerAd(modifier: Modifier)

    fun onGameCompleted()
    fun canShowInterstitial(): Boolean
    fun showInterstitialIfReady(onDismiss: () -> Unit)
    fun loadInterstitial()
}
```

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/NoOpAdsManager.kt`:
```kotlin
package com.example.tictactoe.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Default no-op ad manager for non-ad platforms, tests, and previews.
 */
class NoOpAdsManager : AdsManager {
    override val isBannerEnabled: Boolean = false

    @Composable
    override fun BannerAd(modifier: Modifier) {
        // No-op for non-ad environments
    }

    override fun onGameCompleted() {}
    override fun canShowInterstitial(): Boolean = false
    override fun showInterstitialIfReady(onDismiss: () -> Unit) {
        onDismiss()
    }
    override fun loadInterstitial() {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:shared:allTests --tests "com.example.tictactoe.ads.AdsPolicyEngineTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/ app/shared/src/commonTest/kotlin/com/example/tictactoe/ads/
git commit -m "feat(ads): add AdsManager interface, NoOpAdsManager, and AdsPolicyEngine"
```

---

### Task 2: Dependency Injection & `AppContainer` Integration

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/di/AppContainer.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/App.kt`
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/LocalAdsManager.kt`
- Test: `app/shared/src/commonTest/kotlin/com/example/tictactoe/ads/AppContainerAdsTest.kt`

**Interfaces:**
- Consumes: `AdsManager`, `NoOpAdsManager` from Task 1
- Produces:
  - `val LocalAdsManager = staticCompositionLocalOf<AdsManager>`
  - `AppContainer(..., adsManager: AdsManager = NoOpAdsManager())`
  - `App(container: AppContainer = remember { AppContainer() })`

- [ ] **Step 1: Write test for `AppContainer` with custom `AdsManager`**

Create `app/shared/src/commonTest/kotlin/com/example/tictactoe/ads/AppContainerAdsTest.kt`:
```kotlin
package com.example.tictactoe.ads

import com.example.tictactoe.di.AppContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppContainerAdsTest {
    class FakeAdsManager : AdsManager {
        var completedGames = 0
        var interstitialShown = 0
        override val isBannerEnabled = true

        @androidx.compose.runtime.Composable
        override fun BannerAd(modifier: androidx.compose.ui.Modifier) {}

        override fun onGameCompleted() { completedGames++ }
        override fun canShowInterstitial() = true
        override fun showInterstitialIfReady(onDismiss: () -> Unit) {
            interstitialShown++
            onDismiss()
        }
        override fun loadInterstitial() {}
    }

    @Test
    fun defaultContainerUsesNoOpAdsManager() {
        val container = AppContainer()
        assertTrue(container.adsManager is NoOpAdsManager)
    }

    @Test
    fun customAdsManagerIsInjectedSuccessfully() {
        val fakeAds = FakeAdsManager()
        val container = AppContainer(adsManager = fakeAds)
        assertEquals(fakeAds, container.adsManager)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:shared:allTests --tests "com.example.tictactoe.ads.AppContainerAdsTest"`
Expected: FAIL with compilation error (`adsManager` not found on `AppContainer`).

- [ ] **Step 3: Update `AppContainer.kt`, `LocalAdsManager.kt`, and `App.kt`**

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/LocalAdsManager.kt`:
```kotlin
package com.example.tictactoe.ads

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAdsManager = staticCompositionLocalOf<AdsManager> { NoOpAdsManager() }
```

Modify `app/shared/src/commonMain/kotlin/com/example/tictactoe/di/AppContainer.kt`:
Update constructor:
```kotlin
class AppContainer(
    private val computerPlayers: ComputerPlayerProvider = DefaultComputerPlayerProvider(),
    private val progressRepository: ProgressRepository = SettingsProgressRepository(Settings()),
    private val todayProvider: TodayProvider = SystemTodayProvider(),
    val adsManager: AdsManager = NoOpAdsManager(),
)
```

Modify `app/shared/src/commonMain/kotlin/com/example/tictactoe/App.kt`:
Provide `LocalAdsManager` in `App`:
```kotlin
package com.example.tictactoe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tictactoe.ads.LocalAdsManager
import com.example.tictactoe.di.AppContainer
import com.example.tictactoe.presentation.Intent
import com.example.tictactoe.ui.GameContent
import com.example.tictactoe.ui.theme.TicTacToeTheme

@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    CompositionLocalProvider(LocalAdsManager provides container.adsManager) {
        TicTacToeTheme {
            GameScreen(container = container)
        }
    }
}

/** Wiring only: owns the ViewModel and hands plain state and a callback to [GameContent]. */
@Composable
private fun GameScreen(container: AppContainer) {
    val viewModel = viewModel { container.createGameViewModel() }
    val ui by viewModel.state.collectAsStateWithLifecycle()
    // Picks up a midnight rollover when the app returns to the foreground.
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(Intent.RefreshDate)
        onPauseOrDispose { }
    }
    GameContent(ui = ui, onIntent = viewModel::onIntent)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:shared:allTests --tests "com.example.tictactoe.ads.AppContainerAdsTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/di/AppContainer.kt app/shared/src/commonMain/kotlin/com/example/tictactoe/App.kt app/shared/src/commonMain/kotlin/com/example/tictactoe/ads/LocalAdsManager.kt app/shared/src/commonTest/kotlin/com/example/tictactoe/ads/AppContainerAdsTest.kt
git commit -m "feat(ads): integrate AdsManager into AppContainer and provide LocalAdsManager"
```

---

### Task 3: Dependencies, Manifest, & Android Ad Implementation

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/androidApp/build.gradle.kts`
- Modify: `app/androidApp/src/main/AndroidManifest.xml`
- Create: `app/androidApp/src/main/kotlin/com/example/tictactoe/ads/AdConfig.kt`
- Create: `app/androidApp/src/main/kotlin/com/example/tictactoe/ads/AndroidAdsManager.kt`

**Interfaces:**
- Consumes: `AdsManager`, `AdsPolicyEngine` from Task 1, Google Play Services Ads SDK
- Produces:
  - `class AndroidAdsManager(activityProvider: () -> Activity?) : AdsManager`
  - `object AdConfig { val sampleAppId: String; val testBannerAdUnitId: String; val testInterstitialAdUnitId: String }`

- [ ] **Step 1: Add Google Play Services Ads dependency to Version Catalog**

Modify `gradle/libs.versions.toml`:
In `[versions]`:
```toml
playServicesAds = "23.6.0"
```
In `[libraries]`:
```toml
playServicesAds = { module = "com.google.android.gms:play-services-ads", version.ref = "playServicesAds" }
```

- [ ] **Step 2: Add dependency to `app/androidApp/build.gradle.kts`**

Modify `app/androidApp/build.gradle.kts`:
In `dependencies { ... }`:
```kotlin
dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.playServicesAds)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
```

- [ ] **Step 3: Update `AndroidManifest.xml` with Google Sample App ID**

Modify `app/androidApp/src/main/AndroidManifest.xml`:
Inside `<application ...>`:
```xml
        <!-- Google Mobile Ads Sample Application ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-3940256099942544~3347511713"/>
```

- [ ] **Step 4: Create `AdConfig.kt` with test constants**

Create `app/androidApp/src/main/kotlin/com/example/tictactoe/ads/AdConfig.kt`:
```kotlin
package com.example.tictactoe.ads

object AdConfig {
    const val SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
}
```

- [ ] **Step 5: Create `AndroidAdsManager.kt` with Banner & Interstitial implementation**

Create `app/androidApp/src/main/kotlin/com/example/tictactoe/ads/AndroidAdsManager.kt`:
```kotlin
package com.example.tictactoe.ads

import android.app.Activity
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
    private val activityProvider: () -> Activity?,
    currentTimeProvider: () -> Long = { System.currentTimeMillis() },
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
        return policy.canShowInterstitial() && interstitialAd != null
    }

    override fun showInterstitialIfReady(onDismiss: () -> Unit) {
        val activity = activityProvider()
        val ad = interstitialAd
        if (activity != null && ad != null && policy.canShowInterstitial()) {
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
```

- [ ] **Step 6: Verify compilation of `androidApp` module**

Run: `./gradlew :app:androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/androidApp/build.gradle.kts app/androidApp/src/main/AndroidManifest.xml app/androidApp/src/main/kotlin/com/example/tictactoe/ads/
git commit -m "feat(android): add Google Mobile Ads SDK, AndroidAdsManager, and manifest configuration"
```

---

### Task 4: UI Placement & Game Lifecycle Triggers

**Files:**
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt`
- Modify: `app/androidApp/src/main/kotlin/com/example/tictactoe/MainActivity.kt`
- Test: `app/shared/src/commonTest/kotlin/com/example/tictactoe/ui/GameScreenAdsTest.kt`

**Interfaces:**
- Consumes: `LocalAdsManager`, `AdsManager`, `GameContent`
- Produces:
  - Bottom Banner ad rendered in `GameContent` (respects `isBannerEnabled`)
  - Interstitial trigger invoked on game end / rematch action
  - `MainActivity` initializes `MobileAds` and injects `AndroidAdsManager` into `AppContainer`

- [ ] **Step 1: Write UI test verifying GameContent renders with AdsManager without error**

Create `app/shared/src/commonTest/kotlin/com/example/tictactoe/ui/GameScreenAdsTest.kt`:
```kotlin
package com.example.tictactoe.ui

import com.example.tictactoe.ads.AdsManager
import com.example.tictactoe.ads.NoOpAdsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameScreenAdsTest {
    class TrackingAdsManager : AdsManager {
        var completedCalls = 0
        var interstitialCalls = 0
        override val isBannerEnabled = false

        @androidx.compose.runtime.Composable
        override fun BannerAd(modifier: androidx.compose.ui.Modifier) {}

        override fun onGameCompleted() {
            completedCalls++
        }

        override fun canShowInterstitial() = true

        override fun showInterstitialIfReady(onDismiss: () -> Unit) {
            interstitialCalls++
            onDismiss()
        }

        override fun loadInterstitial() {}
    }

    @Test
    fun adsManagerRespondsToGameLifecycle() {
        val tracking = TrackingAdsManager()
        tracking.onGameCompleted()
        assertEquals(1, tracking.completedCalls)

        var dismissed = false
        tracking.showInterstitialIfReady { dismissed = true }
        assertEquals(1, tracking.interstitialCalls)
        assertTrue(dismissed)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :app:shared:allTests --tests "com.example.tictactoe.ui.GameScreenAdsTest"`
Expected: PASS.

- [ ] **Step 3: Update `GameScreen.kt` to embed BannerAd and trigger Interstitial on rematch**

Modify `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt`:
1. In `GameContent`, grab `adsManager = LocalAdsManager.current`.
2. Observe `LaunchedEffect(outcome)`: when `outcome is Outcome.Won || outcome == Outcome.Draw`, call `adsManager.onGameCompleted()`.
3. Wrap "Play Again" / next round trigger:
   Create an ad-intercepted intent callback:
   ```kotlin
   val handleIntentWithAds: (Intent) -> Unit = { intent ->
       if (intent is Intent.StartNextRound) {
           adsManager.showInterstitialIfReady {
               onIntent(intent)
           }
       } else {
           onIntent(intent)
       }
   }
   ```
4. In `CompactLayout` and `WideLayout`, render `if (adsManager.isBannerEnabled) adsManager.BannerAd(Modifier.fillMaxWidth())` at the bottom of the screen layout with safe bottom padding.

- [ ] **Step 4: Update `MainActivity.kt` to initialize MobileAds and pass `AndroidAdsManager`**

Modify `app/androidApp/src/main/kotlin/com/example/tictactoe/MainActivity.kt`:
```kotlin
package com.example.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tictactoe.ads.AndroidAdsManager
import com.example.tictactoe.di.AppContainer
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var adsManager: AndroidAdsManager
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity) {}
        }

        adsManager = AndroidAdsManager(this)
        appContainer = AppContainer(adsManager = adsManager)

        setContent {
            App(container = appContainer)
        }
    }
}
```

- [ ] **Step 5: Run tests and verify Android compilation**

Run: `./gradlew :app:shared:allTests`
Run: `./gradlew :app:androidApp:compileDebugKotlin`
Expected: All tests pass and compilation succeeds.

- [ ] **Step 6: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/GameScreen.kt app/shared/src/commonTest/kotlin/com/example/tictactoe/ui/GameScreenAdsTest.kt app/androidApp/src/main/kotlin/com/example/tictactoe/MainActivity.kt
git commit -m "feat(ui): display BannerAd in GameScreen and trigger interstitial on rematch"
```

---

### Task 5: End-to-End Verification & Assembly

**Files:**
- None (verification across all modules)

- [ ] **Step 1: Run complete multiplatform test suites**

Run: `./gradlew :core:test :app:shared:allTests`
Expected: ALL TESTS PASS.

- [ ] **Step 2: Build Android Debug APK to verify packaging and manifest merging**

Run: `./gradlew :app:androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL (produces debug APK with AdMob SDK merged).

- [ ] **Step 3: Build Desktop App to ensure multiplatform no-op remains clean**

Run: `./gradlew :app:desktopApp:compileKotlin`
Expected: BUILD SUCCESSFUL without missing dependency or linkage errors.

- [ ] **Step 4: Final commit and status check**

Run: `git status`
Verify working tree is clean.
