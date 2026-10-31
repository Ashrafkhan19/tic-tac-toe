# V1 Part 1: Saved Progress, Daily Challenge, Streak, XP, Medium AI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give players a reason to return daily: persisted stats, a daily forced-win puzzle with a streak, XP and levels, and a Medium difficulty.

**Architecture:** Pure rules (progress model, XP, streak, challenge generator, new use cases) go in `core`. Persistence, the ViewModel/reducer changes and the Compose UI go in `app/shared`. There is no separate home screen: the existing game screen is the home, with a progress strip and a daily-challenge banner added above the board, so the first launch reaches a playable board exactly as fast as today.

**Tech Stack:** Kotlin 2.4.21 Multiplatform, Compose Multiplatform 1.12.2, kotlinx-datetime (`LocalDate`), kotlinx-serialization-json, multiplatform-settings 1.3.0 (key-value storage on Android/iOS/JVM).

**Spec:** `docs/superpowers/specs/2026-10-31-v1-retention-design.md`

**Scope note:** The spec's v1 is split into three plans. This is part 1 (spec steps 1, 2 and the XP/levels/Medium half of step 3). Part 2: themes, share card, local notification. Part 3: analytics, rewarded ads, purchase (needs the owner's accounts). Release gate (owner's decision): nothing ships publicly until part 3 (analytics, ads, purchase) is done, so the owner should create the store, ad network, and analytics accounts while parts 1 and 2 are built. The project is not a git repository, so tasks end with a test run instead of a commit.

## Global Constraints

- Build on the existing code: modules `core` (rules, AI, no UI) and `app/shared` (MVI presentation and Compose UI), use cases plus the `AppContainer` composition root.
- Android and iOS phones and tablets are the priority. Desktop must keep compiling and running but gets no special work.
- No backend in v1. Everything works offline. No mandatory signup before playing.
- Nothing may make the first launch slower to reach a playable board than it is today.
- Dates are stored as local calendar dates (yyyy-MM-dd). Persisted difficulty keys are the enum names `Easy`, `Medium`, `Unbeatable`.
- Only vs-Computer games count towards lifetime stats and XP. 2-player and abandoned games do not.
- Existing tests must keep passing; new rules code gets tests.

## Review Focus

1. Corrupt, empty or future-version saved data: app opens with fresh progress, never crashes.
2. Midnight passes while the app is open: a challenge started before midnight is credited to its own date; the banner updates to the new day on resume.
3. Completing the same day's challenge twice (retry after a win, or a re-launch): XP bonus and streak counted once.
4. A finished round recorded more than once (rotation, ViewModel recreation, repeated state emission): counted once.
5. Clock goes backwards or timezone changes: streak never decreases or double-counts, and a stored date later than today is tolerated.

---

## File Structure

Create in `core/src/commonMain/kotlin/com/example/tictactoe/domain/`:
- `progress/PlayerProgress.kt` — `PlayerProgress`, `DifficultyStats`, `GameResult`
- `progress/XpRules.kt` — XP table and level curve
- `progress/StreakRules.kt` — `StreakState` and its rules
- `progress/ProgressRepository.kt` — interface plus `InMemoryProgressRepository`
- `challenge/ForcedWin.kt` — `movesToForcedWin`
- `challenge/DailyChallenge.kt` — `DailyChallenge`, `DailyChallengeGenerator`
- `time/TodayProvider.kt` — `TodayProvider`, `SystemTodayProvider`
- Modify `model/Marks.kt` (Difficulty), `player/Players.kt` (Medium), `usecase/UseCases.kt` (two use cases)

Create in `app/shared/src/commonMain/kotlin/com/example/tictactoe/`:
- `data/SettingsProgressRepository.kt`
- `ui/components/Progress.kt` — `ProgressStrip`, `DailyChallengeBanner`
- Modify `presentation/GameMvi.kt`, `presentation/GameViewModel.kt`, `di/AppContainer.kt`, `ui/GameScreen.kt`, `ui/components/Controls.kt`, `App.kt`

Tests mirror each file under `core/src/commonTest/.../domain/` and `app/shared/src/commonTest/...`.

---

### Task 1: Medium difficulty

**Files:** Modify `core/.../model/Marks.kt`, `core/.../player/Players.kt`, `app/shared/.../ui/components/Controls.kt`; Test `core/src/commonTest/.../domain/PlayersTest.kt`.

**Interfaces:**
- Produces: `enum class Difficulty { Easy, Medium, Unbeatable }` (this order); `class MistakeProneComputerPlayer(strong: ComputerPlayer, weak: ComputerPlayer, mistakeOneIn: Int, random: Random = Random) : ComputerPlayer` (plays `weak` one time in `mistakeOneIn`, otherwise `strong`; `require(mistakeOneIn >= 1)`).

- [ ] **Step 1: Write failing tests in `PlayersTest.kt`:** `mistakeProneUsesWeakWhenRandomHitsZero` (fake `Random` whose `nextInt(4)` returns 0 → returns the weak player's move), `mistakeProneUsesStrongOtherwise` (returns 1 → strong's move), `mistakeOneInBelowOneRejected` (`assertFailsWith<IllegalArgumentException>`), and `providerHasPlayerForMedium` (`DefaultComputerPlayerProvider().playerFor(Difficulty.Medium)` is not the same instance as the `Unbeatable` and `Easy` players).
- [ ] **Step 2:** Run `./gradlew :core:jvmTest --tests '*PlayersTest*'` — expect compile failure / FAIL.
- [ ] **Step 3:** Add `Medium` to the enum; implement `MistakeProneComputerPlayer`; register `Difficulty.Medium to MistakeProneComputerPlayer(perfect, RandomPlayer(), mistakeOneIn = 4)` in `defaultPlayers()`.
- [ ] **Step 4:** In `ModeSelector` (`Controls.kt`) add `"Medium" to Difficulty.Medium` between Easy and Unbeatable.
- [ ] **Step 5:** Run `./gradlew :core:jvmTest :app:shared:jvmTest` — expect all PASS (the existing `DefaultComputerPlayerProvider` "every difficulty needs a player" check must still pass).

### Task 2: Progress model, XP and levels, record-game use case

**Files:** Create `progress/PlayerProgress.kt`, `progress/XpRules.kt`; Modify `core/build.gradle.kts`, `gradle/libs.versions.toml`, `usecase/UseCases.kt`; Test `core/src/commonTest/.../domain/ProgressTest.kt`.

**Interfaces:**
- Produces (package `...domain.progress`, all `@Serializable`): `data class DifficultyStats(wins: Int = 0, draws: Int = 0, losses: Int = 0)`; `data class PlayerProgress(version: Int = 1, xp: Int = 0, stats: Map<Difficulty, DifficultyStats> = emptyMap(), streak: StreakState = StreakState())`; `enum class GameResult { Win, Draw, Loss }`.
- Produces: `object XpRules { const val CHALLENGE_BONUS = 50; fun forGame(difficulty: Difficulty, result: GameResult): Int; fun levelFor(xp: Int): Int; fun xpToReach(level: Int): Int }`.
- Produces: `class RecordGameResult { operator fun invoke(progress: PlayerProgress, settings: MatchSettings, outcome: Outcome): PlayerProgress }` (in `usecase`).
- Also produces here: the plain `@Serializable data class StreakState(current: Int = 0, best: Int = 0, freezes: Int = 0, lastCompleted: LocalDate? = null)` in `progress/StreakRules.kt`, with no rules yet; Task 3 adds the rules to the same file.

- [ ] **Step 1: Add dependencies.** In `libs.versions.toml` add `kotlinx-datetime` (0.7.1), `kotlinx-serialization` (1.7.3) with the `kotlin-serialization` plugin (version `kotlin`), and `multiplatform-settings` (1.3.0) with modules `multiplatform-settings`, `-no-arg`, `-test`. In `core/build.gradle.kts` apply the serialization plugin and add `kotlinx-datetime` and `kotlinx-serialization-json` to `commonMain`. If multiplatform-settings 1.3.0 does not resolve against Kotlin 2.4.21, use the newest release that does; report which.
- [ ] **Step 2: Write failing tests** (`ProgressTest.kt`), exact values:
  - XP table: Win = 10/20/40, Draw = 3/6/15, Loss = 1/1/1 for Easy/Medium/Unbeatable.
  - `levelFor(0) == 1`; for n in 2..30 `levelFor(xpToReach(n)) == n` and `levelFor(xpToReach(n) - 1) == n - 1`; `xpToReach(1) == 0`; `xpToReach` strictly increasing.
  - `RecordGameResult`: vs-Computer Medium, `Won(Mark.X, …)` → `stats[Medium].wins == 1`, `xp == 20`; `Won(Mark.O, …)` → `losses == 1`, `xp == 1`; `Draw` on Unbeatable → `draws == 1`, `xp == 15`; `InProgress` → progress unchanged; `Mode.TwoPlayer` with any outcome → progress unchanged.
- [ ] **Step 3:** Run `./gradlew :core:jvmTest --tests '*ProgressTest*'` — expect FAIL.
- [ ] **Step 4:** Implement the types and rules. The human is X in vs-Computer. Level curve is the builder's choice but must satisfy the tests above (suggest a gentle quadratic, e.g. 100 XP for level 2, growing).
- [ ] **Step 5:** Run `./gradlew :core:jvmTest` — expect PASS.

### Task 3: Streak rules and challenge completion

**Files:** Create `progress/StreakRules.kt`; Modify `usecase/UseCases.kt`; Test `core/src/commonTest/.../domain/StreakTest.kt`.

**Interfaces:**
- Consumes: `StreakState` data class from Task 2.
- Produces: `fun StreakState.completed(on: LocalDate): StreakState`; `fun StreakState.displayed(today: LocalDate): Int`; `fun StreakState.isAtRisk(today: LocalDate): Boolean`.
- Produces: `class CompleteDailyChallenge { operator fun invoke(progress: PlayerProgress, challengeDate: LocalDate): PlayerProgress }`.

Rules for `completed(on)` (let `gap = on - lastCompleted` in days):
- `lastCompleted == null` → current 1.
- `gap <= 0` → unchanged (same day again, or a date earlier than the last).
- `gap == 1` → current + 1.
- `gap == 2` and `freezes > 0` → current + 1, freezes − 1.
- otherwise → current 1 (freezes untouched).
- After incrementing, if the new current is a multiple of 7, `freezes = min(1, freezes + 1)`. `best = max(best, current)`; `lastCompleted = on`.
`displayed(today)`: 0 if never completed; `current` if gap ≤ 1, or gap == 2 with a freeze banked; else 0 (negative gap, i.e. clock moved back, shows `current`). `isAtRisk(today)`: `current > 0 && lastCompleted != today`.
`CompleteDailyChallenge`: if `streak.lastCompleted` is on or after `challengeDate` return unchanged (idempotent, no second bonus); else apply `completed` and add `XpRules.CHALLENGE_BONUS` XP.

- [ ] **Step 1: Write failing tests** (dates e.g. 2026-11-01 onward): first completion → current 1, best 1; same day twice → unchanged; next day → 2; gap 2 with 1 freeze → current+1 and freezes 0; gap 2 with no freeze → current 1; gap 3 with 1 freeze → current 1 and freeze still 1; earlier date → unchanged; 7th consecutive day → freezes 1; 14th day with freeze already banked → freezes still 1; `best` survives a reset; `displayed` is 0 after a 3-day gap and `current` after a 2-day gap with a freeze; `isAtRisk` true yesterday-completed/not today, false once completed today; `CompleteDailyChallenge` twice on the same date adds `CHALLENGE_BONUS` exactly once.
- [ ] **Step 2:** Run `./gradlew :core:jvmTest --tests '*StreakTest*'` — expect FAIL.
- [ ] **Step 3:** Implement per the rules above.
- [ ] **Step 4:** Run `./gradlew :core:jvmTest` — expect PASS.

### Task 4: Daily challenge generator

**Files:** Create `challenge/ForcedWin.kt`, `challenge/DailyChallenge.kt`, `time/TodayProvider.kt`; Test `core/src/commonTest/.../domain/DailyChallengeTest.kt`.

**Interfaces:**
- Produces: `fun movesToForcedWin(game: GameState): Int?` — with X to move, the number of X moves until X wins when X plays fastest and O delays longest; `null` if O can avoid losing or game is over.
- Produces: `data class DailyChallenge(val date: LocalDate, val game: GameState, val movesToWin: Int)`; `class DailyChallengeGenerator { fun forDate(date: LocalDate): DailyChallenge }`; `fun interface TodayProvider { fun today(): LocalDate }` and `class SystemTodayProvider : TodayProvider` (system default time zone).
- Weekday targets for `movesToWin`: Mon 1, Tue 2, Wed 2, Thu 2, Fri 3, Sat 3, Sun 3.

Approach: enumerate every unfinished position with X to move reachable with 2, 4 or 6 marks placed, group by `movesToForcedWin`, sort each pool deterministically, pick with `Random(date.toEpochDays())`. If a target's pool is empty, fall back to the nearest lower target and tell the owner which weekdays changed.

- [ ] **Step 1: Write failing tests:** `movesToForcedWin(play(0, 3, 1, 4)) == 1`; empty board → `null`; for 14 consecutive dates starting 2026-11-01: `challenge.game.turn == Mark.X`, `!game.isOver`, `movesToForcedWin(game) == challenge.movesToWin`, and `movesToWin` equals the weekday target; `forDate(d) == forDate(d)`; the 14 boards are not all identical; the generator never returns a board where O has already won.
- [ ] **Step 2:** Run `./gradlew :core:jvmTest --tests '*DailyChallengeTest*'` — expect FAIL.
- [ ] **Step 3:** Implement. Cache the pools lazily.
- [ ] **Step 4:** Run `./gradlew :core:jvmTest` — expect PASS. Print the pool sizes once while developing to confirm the targets 2 and 3 exist; if 3 has no positions, change the table and report it.

### Task 5: Persistence

**Files:** Create `core/.../progress/ProgressRepository.kt`, `app/shared/.../data/SettingsProgressRepository.kt`; Modify `app/shared/build.gradle.kts`; Test `app/shared/src/commonTest/.../data/SettingsProgressRepositoryTest.kt`.

**Interfaces:**
- Produces: `interface ProgressRepository { fun load(): PlayerProgress; fun save(progress: PlayerProgress) }`; `class InMemoryProgressRepository(initial: PlayerProgress = PlayerProgress()) : ProgressRepository`; `class SettingsProgressRepository(settings: Settings, json: Json = Json { ignoreUnknownKeys = true }) : ProgressRepository` storing one JSON string under key `player_progress_v1`.

- [ ] **Step 1: Write failing tests** with `MapSettings`: round-trip of a progress with xp, stats for all three difficulties, and a streak with a date; empty storage → `PlayerProgress()`; garbage string `"{not json"` → `PlayerProgress()` and no exception; JSON with an unknown extra field and a higher `version` → loads the known fields; `save` then a new repository instance on the same settings returns the saved value.
- [ ] **Step 2:** Run `./gradlew :app:shared:jvmTest --tests '*SettingsProgressRepositoryTest*'` — expect FAIL.
- [ ] **Step 3:** Implement; `load()` catches `SerializationException`/`IllegalArgumentException` and returns defaults. Add `multiplatform-settings` and `-no-arg` to `commonMain`, `-test` to `commonTest`.
- [ ] **Step 4:** Run the same command — expect PASS. Also run `./gradlew :app:androidApp:assembleDebug` and `./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64` to confirm the Settings dependency builds on Android and iOS (`Settings()` from the no-arg artifact needs no manual Android initialization).

### Task 6: ViewModel and reducer integration

**Files:** Modify `presentation/GameMvi.kt`, `presentation/GameViewModel.kt`, `di/AppContainer.kt`, `App.kt`; Test `app/shared/src/commonTest/.../presentation/GameViewModelTest.kt`, `GameReducerTest.kt`.

**Interfaces:**
- Consumes: Tasks 2–5.
- Produces: `UiState` gains `progress: PlayerProgress`, `today: LocalDate`, `todayChallenge: DailyChallenge`, `inChallenge: Boolean`; the reducer must now `state.copy(match = …)` instead of building a fresh `UiState`. New intents: `Intent.StartDailyChallenge`, `Intent.LeaveDailyChallenge`, `Intent.RefreshDate`. `GameViewModel` gains constructor parameters `repository: ProgressRepository`, `recordGameResult: RecordGameResult`, `completeDailyChallenge: CompleteDailyChallenge`, `today: TodayProvider`, `challenges: DailyChallengeGenerator`.
- Behaviour: `StartDailyChallenge` sets `inChallenge = true` and `match = Match(MatchSettings(VsComputer, Unbeatable), game = todayChallenge.game)`. While `inChallenge`, `NewRound` restarts the same position and `ModeSelected`/`DifficultySelected` are ignored. `LeaveDailyChallenge` returns to a fresh free-play `Match`. `RefreshDate` re-reads `today` and regenerates `todayChallenge` (not while a challenge round is in progress). `ResetScores` resets only the session scoreboard, never `progress`.
- When a round goes from not over to over: in free play call `recordGameResult`; in a challenge, if X won call `completeDailyChallenge(progress, todayChallenge.date)` (nothing is recorded for a challenge draw or loss, and challenge games do not touch per-difficulty stats). Save through `repository` and put the result in `UiState.progress`.

- [ ] **Step 1: Write failing tests** (fake `TodayProvider`, `InMemoryProgressRepository`, `thinkTimeMs = 0`): finishing a free Easy game that X wins saves 10 XP and one win; a 2-player game saves nothing; the same finished state dispatched again (no new move) records once; starting the challenge sets `inChallenge` and the preset board; winning the challenge sets `streak.current == 1`, adds 50 XP and persists; winning it again after `NewRound` adds nothing more; losing the challenge changes nothing and `NewRound` restores the original position; a challenge started on day D and finished after `RefreshDate` moved today to D+1 is credited to D; `LeaveDailyChallenge` restores free play; the ViewModel built on a repository that already holds progress exposes it in `state`.
- [ ] **Step 2:** Run `./gradlew :app:shared:jvmTest` — expect FAIL.
- [ ] **Step 3:** Implement. Detect "round just finished" in `GameViewModel.dispatch` by comparing the state before and after the reduce. Wire the new collaborators in `AppContainer`, defaulting to `SettingsProgressRepository(Settings())` and `SystemTodayProvider()`.
- [ ] **Step 4:** In `App.kt`, send `Intent.RefreshDate` on lifecycle resume.
- [ ] **Step 5:** Run `./gradlew :core:jvmTest :app:shared:jvmTest` — expect all PASS, including all the original tests.

### Task 7: Progress strip, challenge banner, challenge mode UI

**Files:** Create `ui/components/Progress.kt`; Modify `ui/GameScreen.kt`, `ui/components/Controls.kt`.

**Interfaces:** Consumes `UiState` and `Intent` from Task 6; `StreakState.displayed/isAtRisk`, `XpRules.levelFor/xpToReach`.

- [ ] **Step 1:** `ProgressStrip(ui)`: streak count with a flame glyph (dimmed when 0, accent when `isAtRisk`), "Level N", and an XP progress bar from `xpToReach(level)` to `xpToReach(level + 1)`. `DailyChallengeBanner(ui, onIntent)`: "Daily challenge · win in N" with a **Play** button, or a "Done today" state once `streak.lastCompleted == todayChallenge.date`; hidden while `inChallenge`.
- [ ] **Step 2:** In `GameScreen.kt` place both under `Header` in `CompactLayout` and in the right column of `WideLayout`. When `inChallenge`, show "Daily challenge" in place of the mode selector and a **Back to free play** button (`Intent.LeaveDailyChallenge`); the **New round** button reads **Retry**.
- [ ] **Step 3:** Add `@Preview`s for: new player, mid-streak at risk, challenge done, and in-challenge. Check phone portrait, tablet portrait and landscape previews do not overflow; the page already scrolls vertically in compact layout.
- [ ] **Step 4:** Run `./gradlew :app:shared:jvmTest :app:androidApp:assembleDebug` — expect PASS / BUILD SUCCESSFUL.

### Task 8: End-to-end check on a real runtime

- [ ] **Step 1:** Run `./gradlew :core:jvmTest :app:shared:jvmTest :core:testAndroidHostTest :app:shared:testAndroidHostTest :app:shared:iosSimulatorArm64Test` — all PASS.
- [ ] **Step 2:** Run the app (Android emulator or iOS simulator). Verify, with screenshots: first launch shows the board immediately with a level-1 strip and the challenge banner; win a game on Medium → XP and bar increase; kill and relaunch → XP and stats are still there; play today's challenge to a win → streak shows 1, banner says done, bonus XP added; kill and relaunch → still 1; lose the challenge → streak unchanged and **Retry** restores the position; tablet width shows the wide layout with strip and banner.
- [ ] **Step 3:** Confirm `./gradlew :app:desktopApp:run` still starts (smoke only).
- [ ] **Step 4:** Report any of: weekday targets that had to change (Task 4), the multiplatform-settings version actually used (Task 2), and anything not verified.
