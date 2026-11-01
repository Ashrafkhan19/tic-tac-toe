# Mind Gym: Cognitive Focus & Anti-Dopamine Training Design Spec

## Intent & Overview
Continuous short-form video consumption (Reels, TikTok, Shorts) conditions the human brain to expect instant dopamine rewards every 10–30 seconds. This results in attentional fragmentation, reduced cognitive stamina for low-stimulation tasks, and chronic mental fatigue ("brain rot").

This project evolves the existing Tic-Tac-Toe multiplatform codebase into **Mind Gym**, a clinically and neurobiologically grounded cognitive training suite designed to break compulsive doomscrolling reflexes, optimize brain-body recovery, and rebuild sustained attention:
1. **Scientific Foundations:** Every module is anchored in peer-reviewed neuroscience literature (2023–2024).
2. **Urge Surfer (Instant SOS):** A 60-second guided physiological reset to down-regulate autonomic dopamine cravings.
3. **Neuro Lab:** Executive function drills—Stroop inhibitory control and 2-back working memory.
4. **Deep Reading Sanctuary:** Distraction-free, scroll-free paginated long-form reading with active recall checks.
5. **Deep Work Sanctuary:** Distraction-logging focus timer with calm ambient audio.
6. **Strategic Gym:** The existing Tic-Tac-Toe engine (Classic 3×3, 4×4, 5×5, and Ultimate Tic-Tac-Toe) repurposed as working-memory and forward-planning training.
7. **Body & Brain Foundations (Sleep & Movement):** Daily tracking of Slow-Wave Sleep quality and aerobic BDNF priming.
8. **Daily Focus Circuit:** A 10–12 minute guided daily workout tying all exercises into the app's persistent streak and XP engine.

---

## Scientific Foundations & Research Backing (2023–2024)

### 1. Attentional Fragmentation & Short-Form Video (The "Brain Rot" Mechanism)
* **Yan et al. (2024, *Frontiers in Human Neuroscience*):** *"Mobile phone short video use negatively impacts attention functions: An EEG study."* Demonstrated that heavy short-form video consumption significantly diminishes frontal theta wave power during executive control tasks, impairing neural efficiency in the prefrontal cortex.
* **Chen et al. (2023):** Eye-tracking and Stroop behavioral analysis showed that short-video dependence directly impairs inhibitory control and causes visual fragmentation (shorter fixation durations and erratic eye darting).
* **Stanford Digital Repository (2024):** High-frequency short-video use correlates with elevated reaction time variability (the clinical biomarker for attentional micro-lapses) and degraded working memory recall.

### 2. Autonomic Down-Regulation (Urge Surfing)
* **Balban, Huberman et al. (2023, *Cell Reports Medicine*):** *"Brief structured respiration practices enhance mood and reduce physiological arousal."* A randomized controlled trial of 108 subjects proved that cyclic sighing (two nasal inhales followed by one slow oral exhale) for 5 minutes significantly outperforms mindfulness meditation in rapidly lowering autonomic arousal, reducing heart rate, and restoring calm alertness.

### 3. Text Processing & Active Recall (Deep Reading)
* **Altamura, Salmerón et al. (2023/2024, *Review of Educational Research* & *J. Educ. Psych.*):** Meta-analysis of 470,000+ individuals establishing the "Screen Inferiority Effect". Continuous vertical scrolling induces cognitive overload and destroys spatial mental mapping of text, resulting in shallow skimming. Static, paginated reading preserved deep narrative retention.
* **Roediger & Karpicke (Testing Effect):** Active recall / retrieval practice immediately following reading yields up to a 50% increase in long-term memory consolidation compared to passive rereading.

### 4. Sleep & Memory Consolidation (The Foundation)
* **Nature Communications & Frontiers in Neuroscience (2023–2024):** Slow-Wave Sleep (SWS) orchestrates temporal coupling between cortical slow oscillations (<1 Hz), thalamocortical sleep spindles (11–16 Hz), and hippocampal sharp-wave ripples. This physical coupling replays daytime memories and transfers them to the permanent neocortex while glymphatic channels flush metabolic waste from the prefrontal cortex.

### 5. Aerobic Exercise & Neurogenesis (BDNF)
* **Ye et al. (2024, *Frontiers in Neuroscience*):** Aerobic exercise triggers systemic release of Brain-Derived Neurotrophic Factor (BDNF), VEGF, and IGF-1, stimulating neurogenesis and synaptic plasticity in the hippocampus and prefrontal cortex. A 20-minute moderate aerobic session immediately enhances cognitive flexibility, processing speed, and working memory capacity for the next 2–3 hours.

---

## Information Architecture & Navigation

The app introduces a central **Mind Gym Hub** (`GymScreen`) with unified multi-tab navigation:

```text
┌─────────────────────────────────────────────────────────────┐
│                          MIND GYM                           │
│   [ 🔥 Streak: 5 Days ]     [ ⚡ Focus XP: 1,420 ]          │
├─────────────────────────────────────────────────────────────┤
│   [ 🚨 SURF AN URGE (60s RESET) ]                           │
├─────────────────────────────────────────────────────────────┤
│   DAILY FOUNDATIONS (BODY & BRAIN)                          │
│   [ 😴 7-8h Sleep: ✓ ]       [ 🏃 20m Movement: + Log ]    │
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
* **`GymScreen` (Home Floor):** Daily Foundations checklist, Daily Circuit hero card, immediate SOS button, and grid cards for open gym modules.
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

### 5. Daily Foundations (Sleep & Movement Tracker)
* Two quick habit toggles on the Gym Floor:
  * 😴 **Sleep Optimization:** Toggles whether 7–8 hours of quality sleep was attained (+15 XP).
  * 🏃 **Movement & BDNF Priming:** Toggles whether 20+ minutes of aerobic activity or physical exercise was completed (+15 XP).
* Includes scientific rationale pop-ups explaining how sleep consolidates memory schemas and how exercise elevates neurotrophic factors (BDNF).

### 6. Daily Focus Circuit
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
    val urgeWarmupCompleted: Boolean = false,
    val neuroDrillCompleted: Boolean = false,
    val deepActivityCompleted: Boolean = false,
    val sleepLogged: Boolean = false,
    val movementLogged: Boolean = false
) {
    val isComplete: Boolean get() = urgeWarmupCompleted && neuroDrillCompleted && deepActivityCompleted
}
```

### 2. Integration with Existing `PlayerProgress`
`PlayerProgress` is extended without breaking existing serialized settings:
* Add `totalFocusMinutes: Int = 0`
* Add `urgesSurfedCount: Int = 0`
* Add `circuitsCompletedCount: Int = 0`
* Add `sleepLogsCount: Int = 0`
* Add `movementLogsCount: Int = 0`
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
* Daily Foundations (Sleep & Movement) toggles and scientific insights.
* Integration of the existing Tic-Tac-Toe variants into the Strategic Gym module.
* XP and Streak rewards for all cognitive sessions.

### Out of Scope (Future Releases):
* System-wide Android Accessibility Service app-blocking (relying instead on voluntary standalone training to avoid invasive permission requirements).
* Cloud backend synchronization (app remains 100% offline and private).
