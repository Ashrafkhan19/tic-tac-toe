# Mind Gym & Tic-Tac-Toe Architecture Guide

This document outlines the architecture, directory structure, module boundaries, and design conventions of the project to ensure readability, scalability, and clean separation of concerns.

---

## 1. High-Level Modular Design

The codebase is organized into multiplatform Gradle modules:

```
├── core/                               # Pure Kotlin Domain & Business Logic (zero UI / Android dependencies)
│   ├── src/commonMain/kotlin/
│   │   └── com/example/tictactoe/domain/
│   │       ├── focus/                  # Cognitive rehab, brain drill engines, benchmarks
│   │       ├── challenge/              # Daily puzzles & forced-win challenges
│   │       ├── model/                  # Tic-Tac-Toe & Ultimate boards and rules
│   │       ├── player/                 # AI heuristic players
│   │       ├── progress/               # XP, streaks, levels, PlayerProgress model
│   │       ├── time/                   # Platform-agnostic date/time provider
│   │       └── usecase/                # Application use cases
│   └── src/commonTest/kotlin/          # Comprehensive domain unit tests
│
├── app/shared/                         # Shared Compose Multiplatform UI, Navigation, DI & State
│   ├── src/commonMain/kotlin/
│   │   └── com/example/tictactoe/
│   │       ├── navigation/             # App navigation routes (ScreenRoute)
│   │       ├── di/                     # Dependency injection (AppContainer)
│   │       ├── data/                   # Persistent repository implementations (multiplatform settings)
│   │       ├── ads/                    # Ad management policies & interfaces
│   │       ├── presentation/           # ViewModels and MVI intent/state reducers
│   │       └── ui/                     # UI components, themes, and screen trees
│   │           ├── focus/              # Mind Gym cognitive rehab feature suite
│   │           │   ├── hub/            # Main dashboard & cognitive analytics radar chart
│   │           │   ├── drills/         # Clinical cognitive exercises (Stroop, Flanker, N-Back, etc.)
│   │           │   ├── restoration/    # Somatic reset, wakeful rest, urge breathwork
│   │           │   └── work/           # Deep work focus timer & reading sanctuary
│   │           ├── components/         # Reusable design system components
│   │           └── theme/              # Color schemes, typography, and styling tokens
│   └── src/commonTest/kotlin/          # UI state, ViewModel, and reducer unit tests
│
└── app/
    ├── androidApp/                     # Android application shell & manifest
    ├── desktopApp/                     # JVM Desktop application launcher
    └── iosApp/                         # iOS Swift app project & Xcode configuration
```

---

## 2. Domain Layer: Single Responsibility Drill Engines

All cognitive drill engines in `core/.../domain/focus/` adhere to the **Single Responsibility Principle (SRP)**. Rather than bundling distinct mechanics into a single monolithic file, each engine lives in its own dedicated file:

| File | Cognitive Ability Tested | Scientific Basis |
| :--- | :--- | :--- |
| `StroopEngine.kt` | Prefrontal inhibitory control | Naming font colors against conflicting semantic words (Stroop Effect). |
| `FlankerEngine.kt` | Selective spatial attention & conflict cost | Central arrow target with congruent/incongruent flanking distractors. |
| `GoNoGoEngine.kt` | Motor response inhibition | Fast-paced Go taps vs. total suppression on No-Go targets. |
| `NBackEngine.kt` | Working memory updating & fluid intelligence | Recognizing visual stimuli presented $N$ steps prior in an ongoing stream. |
| `CorsiBlockEngine.kt` | Visuospatial working memory span | Reproducing expanding spatial tile sequences (forward span). |
| `BackwardSpanEngine.kt` | Working memory manipulation | Reversing sequences in memory before reproduction. |
| `TrailMakingEngine.kt` | Cognitive flexibility & task switching | Alternating between numbers and letters ($1 \rightarrow A \rightarrow 2 \rightarrow B$). |
| `SpeedMathEngine.kt` | Working memory bandwidth & retrieval | Timed rapid mental arithmetic calculations. |
| `MatrixLogicEngine.kt` | Abstract inductive fluid reasoning | Pattern completion based on progressive matrix rules (Raven's matrices). |
| `CognitiveBenchmark.kt` | Peer percentile normative engine | Logistic approximation of normal distribution against clinical populations. |
| `ReadingRepository.kt` | Deep reading comprehension & active recall | Paginated scroll-free reading articles with post-read recall testing. |
| `FocusModels.kt` | Focus XP formulas & daily circuit tracking | Non-dopaminergic reward models for cognitive workouts. |

---

## 3. UI Layer: Categorized Screen Tree

In `app/shared/.../ui/focus/`, screens are categorized by cognitive purpose:

```
ui/focus/
├── hub/
│   ├── GymScreen.kt                     # Floor hub: daily circuit, streaks, foundations
│   └── CognitiveAnalyticsScreen.kt      # 5-axis canvas radar chart & population percentiles
├── drills/
│   ├── NeuroLabScreen.kt                # Drill menu router & launch hub
│   ├── StroopDrillView (in NeuroLab)    # Stroop test UI
│   ├── FlankerDrillScreen.kt            # Eriksen Flanker selective attention UI
│   ├── GoNoGoDrillScreen.kt             # Motor reflex UI
│   ├── NBackDrillScreen.kt              # 2-back visual stream UI
│   ├── CorsiBlockScreen.kt              # Spatial grid sequence reproduction UI
│   ├── TrailMakingScreen.kt             # 1-A-2-B canvas target connection UI
│   ├── SpeedMathScreen.kt               # Rapid mental arithmetic UI
│   ├── BackwardSpanScreen.kt            # Reverse memory sequence UI
│   └── MatrixLogicScreen.kt             # Abstract geometric puzzle selection UI
├── restoration/
│   ├── UrgeSurferScreen.kt              # 60s animated physiological sigh breathwork
│   ├── SomaticResetScreen.kt            # 2-min progressive muscle tension & release
│   └── WakefulRestScreen.kt             # 5-min sensory deprivation synaptic lock
└── work/
    ├── FocusTimerScreen.kt              # Deep work timer with 40Hz gamma entrainment
    └── ReadingSanctuaryScreen.kt        # Scroll-free paginated reader with recall quiz
```

---

## 4. Navigation Architecture

Navigation is decoupled from UI screens via `com.example.tictactoe.navigation.ScreenRoute`:
- Destination routes are strongly typed enums.
- Screen composables are pure and stateless where possible, taking callbacks (e.g. `onBack`, `onDrillComplete`, `onOpenNeuroLab`).
- The root `App.kt` or platform router acts as the coordinator between navigation routes, repositories, and state holders.

---

## 5. How to Add a New Cognitive Feature (Scalability Checklist)

To add a new exercise or assessment:

1. **Create the pure domain engine** in `core/.../domain/focus/<NewDrill>Engine.kt`.
   - Ensure zero UI imports.
   - Keep state deterministic and testable via an optional `seed` parameter.
2. **Add unit tests** in `core/.../domain/focus/<NewDrill>Test.kt`.
   - Verify success paths, error states, and edge conditions.
3. **Build the Compose UI** in `app/shared/.../ui/focus/drills/<NewDrill>Screen.kt` (or under `restoration/` / `work/`).
   - Use theme colors from `LocalGameColors.current`.
   - Expose clean callbacks: `onComplete: (score: Int) -> Unit` and `onBack: () -> Unit`.
4. **Register in `NeuroLabScreen.kt` or `ScreenRoute.kt`**:
   - Add to `enum class NeuroDrillType`.
   - Add a menu card in `NeuroLabMenuView`.
   - Link the screen under `when (activeDrill)`.
5. **Verify with Gradle**:
   ```bash
   ./gradlew :core:jvmTest :app:shared:compileKotlinJvm :app:shared:jvmTest --no-daemon
   ```
