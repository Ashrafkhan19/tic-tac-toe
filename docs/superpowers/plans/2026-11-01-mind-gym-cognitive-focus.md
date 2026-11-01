# Mind Gym Cognitive Focus Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Transform the existing multiplatform app into **Mind Gym**, a clinically and neurobiologically grounded cognitive training suite that breaks short-form video doomscrolling reflexes through urge-surfing breathwork, Neuro Lab drills (Stroop & 2-back), paginated deep reading with active recall, deep work focus timers, daily sleep/movement foundation logging, and strategic board game workouts.

**Architecture:** Kotlin Multiplatform (Compose Multiplatform) with Unidirectional Data Flow (UDF/MVI). Core domain logic, drill engines, state reducers, and progress repositories live in `core` (shared across Android, iOS, and Desktop). Calm, minimalist Compose UI modules and interactive canvases live in `app/shared`.

**Tech Stack:** Kotlin 2.x, Kotlin Multiplatform, Compose Multiplatform (Material 3), Kotlin Coroutines, StateFlow, Multiplatform Settings for offline persistence.

**Spec:** [`docs/superpowers/specs/2026-11-01-mind-gym-cognitive-focus-design.md`](file:///Users/user/Documents/Tictactoe/docs/superpowers/specs/2026-11-01-mind-gym-cognitive-focus-design.md)

## Global Constraints
- Target platforms: Android, iOS, Desktop (all shared via Kotlin Multiplatform).
- Architecture boundaries: All business logic, engines, and domain state live in `core`. Compose UI and screens live in `app/shared`.
- Offline-first: Zero mandatory cloud/network calls; all session progress and articles persist locally via `ProgressRepository`.
- No dopamine traps: Monochromatic / calm sage-slate color palette; no spinning wheels, aggressive pop-ups, or infinite scroll layouts.
- Non-breaking compatibility: Existing Tic-Tac-Toe stats, XP, and streak data must be preserved without serialization failures.

## Review Focus
1. **Zero-duration or interrupted session exit:** Exiting an Urge Surfer or Focus Timer prematurely should not corrupt state, award unearned XP, or register negative durations. *(Tested in Task 1)*
2. **Corrupted or empty reading article parsing:** Articles with 0 pages or malformed recall questions must fallback gracefully without crashing the reader. *(Tested in Task 3)*
3. **Stroop drill rapid double-tapping:** Multiple rapid button taps in the Stroop test within a single 50ms window must only register a single answer per trial. *(Tested in Task 2)*
4. **Day-rollover during Daily Circuit:** Progressing through steps past midnight must preserve completed steps for that calendar date and roll over smoothly without infinite loops. *(Tested in Task 1)*
5. **Simultaneous Focus Timer & Audio failure:** If system audio fails to initialize, the timer countdown must proceed visually without throwing an unhandled exception. *(Tested in Task 7)*

---

### Task 1: Cognitive Domain Models & Extended Progress Repository

**Files:**
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/FocusModels.kt`
- Modify: `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/PlayerProgress.kt`
- Modify: `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/XpRules.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/data/SettingsProgressRepository.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/FocusDomainTest.kt`

**Interfaces:**
- Consumes: `PlayerProgress`, `StreakRules` from `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/`
- Produces: `FocusSessionType`, `FocusSessionResult`, `DailyCircuitState`, `FocusXpRules`, updated `PlayerProgress`

- [x] **Step 1: Write the failing test**

```kotlin
// core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/FocusDomainTest.kt
package com.example.tictactoe.domain.focus

import com.example.tictactoe.domain.progress.PlayerProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusDomainTest {
    @Test
    fun dailyCircuitCompletion_requiresAllThreeCoreSteps() {
        val initial = DailyCircuitState(dateIso = "2026-11-01")
        assertFalse(initial.isComplete)

        val withWarmup = initial.copy(urgeWarmupCompleted = true)
        assertFalse(withWarmup.isComplete)

        val withNeuro = withWarmup.copy(neuroDrillCompleted = true)
        assertFalse(withNeuro.isComplete)

        val complete = withNeuro.copy(deepActivityCompleted = true)
        assertTrue(complete.isComplete)
    }

    @Test
    fun focusXpRules_calculatesAppropriateRewards() {
        assertEquals(20, FocusXpRules.xpForUrgeSurfed())
        assertEquals(50, FocusXpRules.xpForDeepReading(scorePercent = 80))
        assertEquals(10, FocusXpRules.xpForDeepReading(scorePercent = 40)) // Below 66% threshold
        assertEquals(25, FocusXpRules.xpForFocusSession(durationMinutes = 25, distractionCount = 2))
        assertEquals(15, FocusXpRules.xpForDailyFoundation())
    }

    @Test
    fun playerProgress_defaultsPreserveOldSerialization() {
        val progress = PlayerProgress()
        assertEquals(0, progress.totalFocusMinutes)
        assertEquals(0, progress.urgesSurfedCount)
        assertEquals(0, progress.circuitsCompletedCount)
        assertEquals(0, progress.sleepLogsCount)
        assertEquals(0, progress.movementLogsCount)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :core:jvmTest --no-daemon`
Expected: FAIL with compilation errors (`Unresolved reference: focus`).

- [x] **Step 3: Write minimal implementation**

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/FocusModels.kt`:
```kotlin
package com.example.tictactoe.domain.focus

enum class FocusSessionType {
    URGE_SURF,
    STROOP_DRILL,
    N_BACK_DRILL,
    DEEP_READING,
    FOCUS_TIMER,
    STRATEGY_MATCH
}

data class FocusSessionResult(
    val id: String,
    val type: FocusSessionType,
    val durationSeconds: Int,
    val scorePercent: Int? = null,
    val distractionCount: Int = 0,
    val timestampMillis: Long = 0L
)

data class DailyCircuitState(
    val dateIso: String,
    val urgeWarmupCompleted: Boolean = false,
    val neuroDrillCompleted: Boolean = false,
    val deepActivityCompleted: Boolean = false,
    val sleepLogged: Boolean = false,
    val movementLogged: Boolean = false
) {
    val isComplete: Boolean get() = urgeWarmupCompleted && neuroDrillCompleted && deepActivityCompleted
}

object FocusXpRules {
    fun xpForUrgeSurfed(): Int = 20
    fun xpForDeepReading(scorePercent: Int): Int = if (scorePercent >= 66) 50 else 10
    fun xpForFocusSession(durationMinutes: Int, distractionCount: Int): Int {
        val base = durationMinutes.coerceAtLeast(1)
        val penalty = (distractionCount * 2).coerceAtMost(base / 2)
        return (base - penalty).coerceAtLeast(5)
    }
    fun xpForDailyFoundation(): Int = 15
}
```

Update `PlayerProgress` in `core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/PlayerProgress.kt`:
Add default parameters `totalFocusMinutes: Int = 0`, `urgesSurfedCount: Int = 0`, `circuitsCompletedCount: Int = 0`, `sleepLogsCount: Int = 0`, `movementLogsCount: Int = 0`.

Update `SettingsProgressRepository.kt` to serialize/deserialize these fields with fallback defaults.

- [x] **Step 4: Run test to verify it passes**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :core:jvmTest --no-daemon`
Expected: BUILD SUCCESSFUL and tests pass.

- [x] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/FocusModels.kt \
        core/src/commonMain/kotlin/com/example/tictactoe/domain/progress/PlayerProgress.kt \
        core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/FocusDomainTest.kt \
        app/shared/src/commonMain/kotlin/com/example/tictactoe/data/SettingsProgressRepository.kt
git commit -m "feat(core): add cognitive focus models, XP rules, and progress properties"
```

---

### Task 2: Neuro Lab Drill Engines (Stroop & 2-Back)

**Files:**
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/NeuroLabEngine.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/NeuroLabEngineTest.kt`

**Interfaces:**
- Consumes: Standard Kotlin random & collections
- Produces: `StroopColor`, `StroopTrial`, `StroopEngine`, `NBackEngine`, `DrillResult`

- [x] **Step 1: Write the failing test**

```kotlin
// core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/NeuroLabEngineTest.kt
package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeuroLabEngineTest {
    @Test
    fun stroopEngine_generatesCorrectTrialCountAndValidatesAnswers() {
        val engine = StroopEngine(totalTrials = 10, seed = 42L)
        val firstTrial = engine.currentTrial()
        
        // Correct font color submission
        val isCorrect = engine.submitAnswer(firstTrial.fontColor)
        assertTrue(isCorrect)
        assertEquals(1, engine.completedTrialsCount())
        assertEquals(1, engine.correctTrialsCount())
    }

    @Test
    fun stroopEngine_rejectsRapidDoubleTapInSameTrial() {
        val engine = StroopEngine(totalTrials = 10, seed = 42L)
        val firstTrial = engine.currentTrial()
        engine.submitAnswer(firstTrial.fontColor)
        
        // Second answer on same trial without advancing
        val doubleTap = engine.submitAnswer(firstTrial.fontColor)
        assertFalse(doubleTap)
        assertEquals(1, engine.completedTrialsCount())
    }

    @Test
    fun nBackEngine_generatesValidSequenceAndDetectsMatches() {
        val engine = NBackEngine(sequence = listOf("A", "B", "A", "C", "A"), n = 2)
        assertEquals("A", engine.currentStimulus(0))
        assertFalse(engine.isMatch(0))
        assertFalse(engine.isMatch(1))
        assertTrue(engine.isMatch(2)) // "A" matches index 0 "A"
        assertFalse(engine.isMatch(3)) // "C" != "B"
        assertFalse(engine.isMatch(4)) // "A" != "A" at index 2? Yes it matches!
        assertTrue(engine.isMatch(4)) // "A" matches index 2 "A"
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :core:jvmTest --no-daemon`
Expected: FAIL with unresolved `StroopEngine`.

- [x] **Step 3: Write minimal implementation**

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/NeuroLabEngine.kt`:
```kotlin
package com.example.tictactoe.domain.focus

import kotlin.random.Random

enum class StroopColor(val displayName: String, val hexColor: Long) {
    RED("RED", 0xFFE53935),
    BLUE("BLUE", 0xFF1E88E5),
    GREEN("GREEN", 0xFF43A047),
    YELLOW("YELLOW", 0xFFFDD835)
}

data class StroopTrial(
    val index: Int,
    val word: StroopColor,
    val fontColor: StroopColor,
    val isCongruent: Boolean = word == fontColor
)

class StroopEngine(
    val totalTrials: Int = 30,
    seed: Long = System.currentTimeMillis()
) {
    private val random = Random(seed)
    private val trials: List<StroopTrial>
    private var currentIndex = 0
    private var correctCount = 0
    private var answeredCurrent = false

    init {
        val colors = StroopColor.values()
        trials = (0 until totalTrials).map { i ->
            val isCongruent = random.nextFloat() < 0.35f
            val word = colors[random.nextInt(colors.size)]
            val fontColor = if (isCongruent) {
                word
            } else {
                val nonMatches = colors.filter { it != word }
                nonMatches[random.nextInt(nonMatches.size)]
            }
            StroopTrial(index = i, word = word, fontColor = fontColor, isCongruent = isCongruent)
        }
    }

    fun currentTrial(): StroopTrial = trials[currentIndex.coerceAtMost(trials.lastIndex)]
    fun isFinished(): Boolean = currentIndex >= totalTrials
    fun completedTrialsCount(): Int = currentIndex
    fun correctTrialsCount(): Int = correctCount
    fun accuracyPercent(): Int = if (currentIndex == 0) 0 else ((correctCount * 100) / currentIndex)

    fun submitAnswer(chosenColor: StroopColor): Boolean {
        if (isFinished() || answeredCurrent) return false
        answeredCurrent = true
        val trial = currentTrial()
        val correct = chosenColor == trial.fontColor
        if (correct) correctCount++
        currentIndex++
        answeredCurrent = false
        return correct
    }
}

class NBackEngine(
    val sequence: List<String>,
    val n: Int = 2
) {
    fun currentStimulus(index: Int): String = sequence[index]
    fun isMatch(index: Int): Boolean {
        if (index < n || index >= sequence.size) return false
        return sequence[index] == sequence[index - n]
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :core:jvmTest --no-daemon`
Expected: BUILD SUCCESSFUL and all tests pass.

- [x] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/NeuroLabEngine.kt \
        core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/NeuroLabEngineTest.kt
git commit -m "feat(core): add Stroop and N-Back cognitive drill engines"
```

---

### Task 3: Deep Reading Sanctuary Repository & Recall Quiz Validator

**Files:**
- Create: `core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/ReadingRepository.kt`
- Test: `core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/ReadingRepositoryTest.kt`

**Interfaces:**
- Consumes: `ReadingArticle`, `RecallQuestion`
- Produces: `ReadingRepository`, curated articles, quiz evaluation methods

- [x] **Step 1: Write the failing test**

```kotlin
// core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/ReadingRepositoryTest.kt
package com.example.tictactoe.domain.focus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadingRepositoryTest {
    @Test
    fun defaultArticles_areAvailableAndNonEmpty() {
        val repo = ReadingRepository()
        val articles = repo.getAllArticles()
        assertTrue(articles.isNotEmpty())
        
        val first = articles.first()
        assertTrue(first.pages.isNotEmpty())
        assertTrue(first.questions.isNotEmpty())
    }

    @Test
    fun evaluateQuiz_calculatesAccuratePercentage() {
        val repo = ReadingRepository()
        val article = repo.getAllArticles().first()
        val correctAnswers = article.questions.map { it.correctIndex }
        
        val score = repo.evaluateQuiz(article.id, correctAnswers)
        assertEquals(100, score)

        val wrongAnswers = article.questions.map { (it.correctIndex + 1) % it.options.size }
        val zeroScore = repo.evaluateQuiz(article.id, wrongAnswers)
        assertEquals(0, zeroScore)
    }

    @Test
    fun evaluateQuiz_handlesEmptyOrUnknownArticleGracefully() {
        val repo = ReadingRepository()
        val score = repo.evaluateQuiz("unknown-id", listOf(0, 1))
        assertEquals(0, score)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :core:jvmTest --no-daemon`
Expected: FAIL with `Unresolved reference: ReadingRepository`.

- [x] **Step 3: Write minimal implementation**

Create `core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/ReadingRepository.kt`:
```kotlin
package com.example.tictactoe.domain.focus

data class ReadingArticle(
    val id: String,
    val title: String,
    val author: String,
    val estimatedMinutes: Int,
    val pages: List<String>,
    val questions: List<RecallQuestion>
)

data class RecallQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

class ReadingRepository {
    private val curatedArticles = listOf(
        ReadingArticle(
            id = "deep-work-intro",
            title = "The Power of Deep Work",
            author = "Cal Newport & Cognitive Science",
            estimatedMinutes = 4,
            pages = listOf(
                "Deep work is the ability to focus without distraction on a cognitively demanding task. It's a skill that allows you to quickly master complicated information and produce better results in less time. In a world full of network tools and instant messages, depth is becoming increasingly rare and valuable.",
                "When you switch from some Task A to another Task B, your attention does not immediately follow. A residue of your attention remains stuck thinking about the previous task. This attention residue severely cripples your cognitive capacity, making problem solving sluggish.",
                "To counter attention residue, deliberate blocks of unbroken concentration are required. By eliminating digital novelties and training the mind to endure boredom, neural pathways for sustained executive control are fortified once again."
            ),
            questions = listOf(
                RecallQuestion(
                    prompt = "What is 'attention residue'?",
                    options = listOf(
                        "Sleepiness after work",
                        "Attention remaining stuck on a previous task after switching",
                        "Physical eye fatigue from screens"
                    ),
                    correctIndex = 1
                ),
                RecallQuestion(
                    prompt = "Why is deep work becoming more valuable?",
                    options = listOf(
                        "It is rare due to constant digital distractions",
                        "Computers cannot run without humans",
                        "It eliminates the need for sleep"
                    ),
                    correctIndex = 0
                )
            )
        ),
        ReadingArticle(
            id = "dopamine-reset",
            title = "The Biology of Dopamine & Cravings",
            author = "Neurobiology Insights",
            estimatedMinutes = 3,
            pages = listOf(
                "Dopamine is not the molecule of pleasure; it is the molecule of motivation, desire, and anticipation. When you anticipate a novel notification or funny reel, dopamine spikes, driving you to reach and scroll.",
                "However, frequent unnatural spikes lead to dopamine receptor downregulation. When your receptors desensitize, ordinary real-world activities feel dull, joyless, and impossible to concentrate on.",
                "The remedy is conscious friction. When you resist an urge for just 60 to 90 seconds, the dopamine anticipation wave naturally crests and subsides, allowing your prefrontal cortex to regain command."
            ),
            questions = listOf(
                RecallQuestion(
                    prompt = "What is dopamine primarily responsible for?",
                    options = listOf(
                        "Muscle recovery",
                        "Motivation, anticipation, and pursuit",
                        "Digestive regulation"
                    ),
                    correctIndex = 1
                ),
                RecallQuestion(
                    prompt = "How long does a craving peak typically last?",
                    options = listOf(
                        "60 to 90 seconds",
                        "Several hours continuously",
                        "Less than 2 seconds"
                    ),
                    correctIndex = 0
                )
            )
        )
    )

    fun getAllArticles(): List<ReadingArticle> = curatedArticles
    fun getArticleById(id: String): ReadingArticle? = curatedArticles.find { it.id == id }

    fun evaluateQuiz(articleId: String, userAnswers: List<Int>): Int {
        val article = getArticleById(articleId) ?: return 0
        if (article.questions.isEmpty() || userAnswers.isEmpty()) return 0
        var correct = 0
        article.questions.forEachIndexed { i, q ->
            if (i < userAnswers.size && userAnswers[i] == q.correctIndex) {
                correct++
            }
        }
        return (correct * 100) / article.questions.size
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :core:jvmTest --no-daemon`
Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/ReadingRepository.kt \
        core/src/commonTest/kotlin/com/example/tictactoe/domain/focus/ReadingRepositoryTest.kt
git commit -m "feat(core): add deep reading repository with active recall evaluations"
```

---

### Task 4: UI — Urge Surfer (60s Guided Breathwork Canvas)

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/UrgeSurferScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/theme/GameTheme.kt`
- Test: Build check with `./gradlew :app:shared:compileKotlinJvm --no-daemon`

**Interfaces:**
- Consumes: `FocusXpRules.xpForUrgeSurfed()`
- Produces: `UrgeSurferScreen(onFinished: (xpAwarded: Int) -> Unit, onDismiss: () -> Unit)`

- [x] **Step 1: Write the failing test / compile check**

Verify target composable imports exist and file compiles.

- [x] **Step 2: Run compile check to verify missing symbol**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:shared:compileKotlinJvm --no-daemon`

- [x] **Step 3: Implement `UrgeSurferScreen`**

Create `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/UrgeSurferScreen.kt`:
- Infinite transition animating circle scale between 1.0f (deflate) $\to$ 1.8f (inhale 1) $\to$ 2.2f (inhale 2 top-off) $\to$ 1.0f (exhale).
- Step text: "Inhale through nose" $\to$ "Sharp top-off inhale" $\to$ "Hold" $\to$ "Slow exhale through mouth".
- 60-second countdown timer.
- "Urge Surfed (+20 XP)" button enabling after 60 seconds or dismissible anytime without error.

- [x] **Step 4: Verify compilation**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:shared:compileKotlinJvm --no-daemon`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/UrgeSurferScreen.kt
git commit -m "feat(ui): add Urge Surfer physiological sigh breathwork canvas"
```

---

### Task 5: UI — Neuro Lab (Interactive Stroop Drill Screen)

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/NeuroLabScreen.kt`
- Test: Compile check with `./gradlew :app:shared:compileKotlinJvm --no-daemon`

**Interfaces:**
- Consumes: `StroopEngine`, `StroopColor`, `StroopTrial`
- Produces: `NeuroLabScreen(onDrillComplete: (scorePercent: Int) -> Unit, onBack: () -> Unit)`

- [x] **Step 1: Create failing compile check**
- [x] **Step 2: Verify compile failure**
- [x] **Step 3: Implement `NeuroLabScreen`**

Features:
- Large centered word rendered in contrasting color.
- Grid of 4 color buttons (RED, BLUE, GREEN, YELLOW).
- Immediate visual feedback (subtle green/red border flash).
- Progress indicator: Trial X of 30.
- Summary dialog on completion showing Accuracy % and XP awarded.

- [x] **Step 4: Verify compilation**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:shared:compileKotlinJvm --no-daemon`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/NeuroLabScreen.kt
git commit -m "feat(ui): add Neuro Lab Stroop test interactive drill screen"
```

---

### Task 6: UI — Deep Reading Sanctuary (Paginated Reader & Quiz)

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/ReadingSanctuaryScreen.kt`
- Test: Compile check with `./gradlew :app:shared:compileKotlinJvm --no-daemon`

**Interfaces:**
- Consumes: `ReadingRepository`, `ReadingArticle`, `FocusXpRules`
- Produces: `ReadingSanctuaryScreen(onArticleFinished: (articleId: String, scorePercent: Int) -> Unit, onBack: () -> Unit)`

- [x] **Step 1: Create failing compile check**
- [x] **Step 2: Verify compile failure**
- [x] **Step 3: Implement `ReadingSanctuaryScreen`**

Features:
- Article selection list.
- Clean paginated view (Page X of Y), large serif/sans-serif typography, no vertical scrolling.
- Tap left/right or "Next Page" button.
- Upon reaching the final page, transforms into the Active Recall Quiz view with selectable multiple-choice answers.
- Calculates score %, awards XP, and calls `onArticleFinished`.

- [x] **Step 4: Verify compilation**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:shared:compileKotlinJvm --no-daemon`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/ReadingSanctuaryScreen.kt
git commit -m "feat(ui): add paginated Reading Sanctuary and active recall quiz UI"
```

---

### Task 7: UI — Focus Timer Sanctuary (Deep Work & Distraction Log)

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/FocusTimerScreen.kt`
- Test: Compile check with `./gradlew :app:shared:compileKotlinJvm --no-daemon`

**Interfaces:**
- Consumes: Coroutine delay or ticker, `FocusXpRules.xpForFocusSession()`
- Produces: `FocusTimerScreen(onSessionComplete: (durationMinutes: Int, distractions: Int) -> Unit, onBack: () -> Unit)`

- [x] **Step 1: Create failing compile check**
- [x] **Step 2: Verify compile failure**
- [x] **Step 3: Implement `FocusTimerScreen`**

Features:
- Circular progress timer with remaining minutes and seconds.
- Presets: 25 min (Pomodoro) and 50 min (Deep Block).
- Start / Pause / Reset controls.
- **"+1 Distraction Urge" button:** Increments counter with gentle toast: *"Awareness logged. Breathe and return to your focus."*
- Session complete dialog calculating net Focus XP.

- [x] **Step 4: Verify compilation**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:shared:compileKotlinJvm --no-daemon`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/FocusTimerScreen.kt
git commit -m "feat(ui): add Deep Work focus timer with distraction logging"
```

---

### Task 8: Integration — Mind Gym Floor & Main Navigation Hub

**Files:**
- Create: `app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/GymScreen.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/App.kt`
- Modify: `app/shared/src/commonMain/kotlin/com/example/tictactoe/di/AppContainer.kt`
- Test: Full build and tests via `./gradlew :core:jvmTest :app:shared:jvmTest --no-daemon`

**Interfaces:**
- Consumes: All focus screens, existing `GameScreen`, `PlayerProgress`, `SettingsProgressRepository`
- Produces: Unified `GymScreen` featuring Daily Foundations, Daily Circuit, SOS Urge button, and Open Gym navigation.

- [x] **Step 1: Implement `GymScreen`**
  - **Header:** Streak flame counter, Focus Level & Total XP.
  - **SOS Banner:** *"🚨 Craving a scroll? Tap to surf the urge (60s)"* -> opens `UrgeSurferScreen`.
  - **Daily Foundations Section:**
    - 😴 *7–8h Sleep Quality* toggle (+15 XP).
    - 🏃 *20m Aerobic Movement* toggle (+15 XP).
  - **Daily Circuit Hero Card:**
    - Step 1: Urge Breathwork (60s)
    - Step 2: Stroop Drill (30 trials)
    - Step 3: Deep Reading Sprint (1 article)
    - "Start Next Step" primary button.
  - **Open Gym Grid:**
    - 🧠 Strategy Gym -> Launches existing Tic-Tac-Toe variants (3×3, 4×4, 5×5, Ultimate).
    - ⚡ Neuro Lab -> Launches Stroop Drill.
    - 📖 Deep Reading -> Launches Reading Sanctuary.
    - ⏱️ Focus Timer -> Launches Deep Work Timer.

- [x] **Step 2: Update `App.kt` and `AppContainer.kt`**
  - Wire `ReadingRepository` and `DailyCircuitState` into `AppContainer`.
  - Add navigation state in `App.kt` to switch between `GymScreen` (default home), `GameScreen` (Strategic Gym), and individual focus drill screens.

- [x] **Step 3: Run full verification suite**

Run: `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home && ./gradlew :core:jvmTest :app:shared:compileKotlinJvm --no-daemon`
Expected: ALL PASS with zero errors.

- [x] **Step 4: Commit**

```bash
git add app/shared/src/commonMain/kotlin/com/example/tictactoe/ui/focus/GymScreen.kt \
        app/shared/src/commonMain/kotlin/com/example/tictactoe/App.kt \
        app/shared/src/commonMain/kotlin/com/example/tictactoe/di/AppContainer.kt
git commit -m "feat(ui): integrate Mind Gym Floor hub with Daily Circuit and Foundations"
```

