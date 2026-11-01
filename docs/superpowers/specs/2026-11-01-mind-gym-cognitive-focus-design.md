# Mind Gym: Cognitive Focus & Anti-Dopamine Training Design Spec

## Intent & Overview
Continuous short-form video consumption (Reels, TikTok, Shorts) conditions the brain to expect instant dopamine rewards every 10–30 seconds. This results in attentional fragmentation, reduced cognitive stamina for low-stimulation tasks, and mental fatigue ("brain rot").

This project evolves the existing Tic-Tac-Toe multiplatform codebase into **Mind Gym**, a calm cognitive training suite designed to break compulsive doomscrolling reflexes and rebuild sustained attention:
1. **Urge Surfer (Instant SOS):** A 60-second guided physiological reset to interrupt compulsive social media reaching habits.
2. **Neuro Lab:** Executive function drills—Stroop inhibitory control and sustained working memory.
3. **Deep Reading Sanctuary:** Distraction-free, scroll-free paginated long-form reading with active recall checks.
4. **Deep Work Sanctuary:** Distraction-logging focus timer with calm ambient audio.
5. **Strategic Gym:** The existing Tic-Tac-Toe engine (Classic 3×3, 4×4, 5×5, and Ultimate Tic-Tac-Toe) repurposed as working-memory and forward-planning training.
6. **Daily Focus Circuit:** A 10–12 minute guided daily workout tying all exercises into the app's persistent streak and XP engine.

---

## Information Architecture & Navigation

The app introduces a central **Mind Gym Hub** (`GymScreen`) with four main tabs:

```text
┌─────────────────────────────────────────────────────────────┐
│                          MIND GYM                           │
│   [ 🔥 Streak: 5 Days ]     [ ⚡ Focus XP: 1,420 ]          │
├─────────────────────────────────────────────────────────────┤
│   [ 🚨 SURF AN URGE (60s RESET) ]                           │
├─────────────────────────────────────────────────────────────┤
│   TODAY'S FOCUS CIRCUIT                                     │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ Step 1: Urge Breathing (60s)                    ✓   │   │
│   │ Step 2: Stroop Drill (3m)                       ▶   │   │
│   │ Step 3: Deep Reading (6m)                           │   │
│   │ [ Start Step 2 ]                                    │   │
│   └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│   OPEN GYM MODULES                                          │
│   [ 🧠 Strategy ]  [ ⚡ Neuro Lab ]  [ 📖 Reading ] [ ⏱️ Timer ]│
└─────────────────────────────────────────────────────────────┘
  (Tab: Gym Floor)    (Tab: Focus Timer)    (Tab: Progress)
```

### Screen Flow & State
* **`GymScreen` (Home Floor):** Daily circuit hero card, immediate SOS button, and grid cards for open gym modules.
* **`UrgeSurferScreen`:** Fullscreen modal with animated dual-inhale/exhale breathing canvas and grounding prompts.
* **`NeuroLabScreen`:** Selection between **Stroop Inhibitory Drill** and **Visual 2-Back Vigilance Drill**.
* **`ReadingSanctuaryScreen`:** E-reader style horizontal page flip with active comprehension questions upon completion.
* **`FocusTimerScreen`:** Deep work timer with distraction logging and offline ambient sound loops.
* **`GameScreen` (Strategy):** Existing Tic-Tac-Toe game board with 3×3, 4×4, 5×5, and Ultimate variants.

---

## Detailed Module Specifications

### 1. Urge Surfer (Impulse Interrupter)
* **Physiological Mechanism:** The physiological sigh (two brief nasal inhalations followed by a slow oral exhalation) rapidly down-regulates sympathetic nervous system arousal.
* **Execution:**
  * 60-second animated circle: Expand phase 1 (1.5s), Expand phase 2 (1.0s), Hold (0.5s), Deflate (3.5s).
  * Haptic vibration triggers at the start of each breath cycle.
  * Rotating grounding text: *"Notice the urge without judgment. Dopamine cravings peak within 60 seconds and subside."*
  * On completion: Emits `SurfedUrgeEvent`, records +1 to daily resisted urges, and awards +20 Focus XP.

### 2. Neuro Lab (Attention & Inhibitory Control)
* **Drill A: Stroop Color Interference:**
  * Displays color words (e.g., "RED", "BLUE", "GREEN", "YELLOW") printed in congruent or incongruent font colors.
  * User must select the font color from 4 buttons while inhibiting the automatic word-reading impulse.
  * 30 trials per drill. Measures accuracy percentage and response latency.
* **Drill B: Visual 2-Back (Working Memory & Vigilance):**
  * Displays a continuous stream of geometric symbols appearing every 1.5 seconds.
  * User taps "Match" when the current symbol matches the symbol from 2 steps back.
  * 25 trials per session. Tests sustained vigilance and working memory buffer retention.

### 3. Deep Reading Sanctuary
* **Format:** Scroll-free horizontal pagination. Text is divided into 150-word pages.
* **Pacing:** Minimum reading dwell time enforced per page (prevents mindless skimming).
* **Active Recall Quiz:**
  * After the final page, 3 comprehension questions appear.
  * User selects correct answers to test encoding rather than passive recognition.
  * Awards +50 Focus XP on $>66\%$ comprehension.
* **Initial Content Library:** 5 curated public-domain essays on deep work, stoicism, digital minimalism, and cognitive resilience.

### 4. Deep Work Sanctuary (Focus Timer)
* **Timer Options:** 25-minute Pomodoro, 50-minute Deep Block, or open Stopwatch.
* **Distraction Logging:**
  * Prominent "+1 Distraction Urge" button.
  * When pressed, plays a subtle haptic tap, increments the distraction count for this session, and displays: *"Awareness logged. Gently return to your work."*
* **Ambient Audio:**
  * Built-in generative audio synthesis or looped offline assets: Brown Noise, Soft Rain, 40Hz Isochronic Tone, or Silence.

### 5. Daily Focus Circuit
* **Daily Sequence:**
  1. 1 min Urge Surfer / Breathing Warmup
  2. 3 min Neuro Drill (Stroop or 2-Back)
  3. 6 min Deep Reading Sprint OR 1 Match of Ultimate Tic-Tac-Toe
* **Completion Reward:** Counts toward the existing `PlayerProgress.streak` and completes the `DailyChallenge`.

---

## Domain & Data Architecture (`core` module)

### 1. New Domain Models (`core/src/commonMain/kotlin/com/example/tictactoe/domain/focus/`)
```kotlin
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
    val scorePercent: Int?,
    val distractionCount: Int,
    val timestampMillis: Long
)

data class ReadingArticle(
    val id: String,
    val title: String,
    val author: String,
    val pages: List<String>,
    val questions: List<RecallQuestion>
)

data class RecallQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

data class DailyCircuitState(
    val dateIso: String,
    val urgeWarmupCompleted: Boolean,
    val neuroDrillCompleted: Boolean,
    val deepActivityCompleted: Boolean
) {
    val isComplete: Boolean get() = urgeWarmupCompleted && neuroDrillCompleted && deepActivityCompleted
}
```

### 2. Integration with Existing `PlayerProgress`
`PlayerProgress` is extended without breaking existing serialized settings:
* Add `totalFocusMinutes: Int = 0`
* Add `urgesSurfedCount: Int = 0`
* Add `circuitsCompletedCount: Int = 0`
* Completing a Daily Circuit or winning a Tic-Tac-Toe match both feed the unified `StreakRules` and `XpRules`.

---

## Error Handling & Edge Cases
1. **Timer Interruption / App Backgrounding:**
   * Focus timer tracks elapsed time using system wall-clock timestamps (`currentTimeMillis`), so backgrounding or receiving a phone call does not corrupt session duration.
2. **Reading Session Exit:**
   * If user exits mid-article, state saves the current page index. Resuming returns to the current page.
3. **Audio Playback Failure:**
   * Audio failure (e.g. audio device busy) gracefully defaults to visual-only timer without crashing or blocking the session.
4. **Data Persistence Compatibility:**
   * `SettingsProgressRepository` handles schema updates with default fallback values, ensuring previous user streaks and scores remain intact.

---

## Scope Boundaries

### In Scope for MVP:
* New `GymScreen` top-level hub with unified navigation.
* Interactive 60-second `UrgeSurferScreen` with animated breath circle & haptics.
* `NeuroLabScreen` featuring Stroop Color interference drill and scoring.
* `ReadingSanctuaryScreen` with paginated reader and active recall quiz.
* `FocusTimerScreen` with countdown and distraction logging.
* Integration of the existing Tic-Tac-Toe variants into the Strategic Gym module.
* XP and Streak rewards for all cognitive sessions.

### Out of Scope (Future Releases):
* System-wide Android Accessibility Service app-blocking (relying instead on voluntary standalone training to avoid invasive permission requirements).
* Cloud backend synchronization (app remains 100% offline and private).
