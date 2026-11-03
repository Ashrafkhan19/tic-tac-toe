# Science content from JSON — design

## Intent
All scientific content shown in the app (facts, claims, papers, norms, articles) currently lives as Kotlin string literals scattered across screens and `core`. It is hard to update and most claims have no verifiable source. Move it into one bundled JSON file in which every claim is backed by a verified source, and make the app read from it.

## Goals
- One bundled `science.json` is the single source of truth for science content.
- Every claim shown to the user has a verified source (title, authors, year, DOI, URL) taken from PubMed or Crossref.
- Claims that cannot be verified are removed from the UI (not marked "pending").
- Content can be updated by editing the JSON without touching Kotlin screens.
- Works offline; no network call at runtime.

## Non-goals
- Runtime download/update of content from a server (deferred; the loader should not make it hard later, e.g. keep parsing separate from where bytes come from).
- Localization.
- Redesigning screens beyond what is needed (see UI changes).

## Anti-goals
- Invented or approximated citations. If a source cannot be found and checked, the claim goes.
- App depending on a live API for text on screen.

## Decisions (user's)
- Today the builder fetches/verifies sources (PubMed/Crossref, free) and bundles the result. Hosted updates come later.
- Unverifiable claims are removed, not flagged.
- Cognitive Analytics: where no published norm can be found for a test, hide the percentile and show the raw score and trend only.

## Content covered (current locations)
- Gym screen "Neuroscience Foundation" dialog insights (sleep/slow-wave, aerobic/BDNF), card descriptions (`GymScreen.kt`).
- Drill card descriptions (`NeuroLabScreen.kt`, 9 drills) and other drill intro text.
- Timed facts/guidance: `WakefulRestScreen`, `UrgeSurferScreen`, `SomaticResetScreen`; `FocusTimerScreen` 40Hz GENUS line.
- Score-based result/feedback messages with brain-region claims (Flanker, Stroop, Corsi, Reading, Trail Making, Backward Span, Matrix Logic, GoNoGo, NBack).
- Benchmark norms (mean/SD per test) and tier labels in `core/.../CognitiveBenchmark.kt`; metric card copy in `CognitiveAnalyticsScreen.kt`.
- Reading articles + recall questions (`core/.../ReadingRepository.kt`). Article claims that are science facts also need sources.

## Approach (builder's call unless noted)
- JSON file `science.json` with sections: insights, drills, timedFacts, feedback, norms, articles. Each claim-bearing entry has `sources: [{title, authors, year, doi, url}]`. Entry ids are stable.
- Load via Compose Multiplatform resources (`composeResources/files/science.json`) in `app/shared`, decode with kotlinx.serialization (already present), expose through a small repository/CompositionLocal. `core` cannot read resources, so loaded data (norms, articles) is passed into `core` from `app/shared`.
- Mark content models `@Serializable`; keep a hardcoded-free fallback: if the file fails to parse, the science UI sections hide rather than crash.
- Gym insight dialog gets a "Sources" section listing papers (tapping a link may open the DOI URL; builder's call on whether links open).
- Cognitive Analytics: percentile shown only for tests with a sourced norm; otherwise raw score and trend.
- Tests: JSON schema/decoding test, test that every entry that makes a claim has at least one source, `CognitiveBenchmark` tests updated for data-driven norms.

## Constraints
- KMP targets: Android, JVM desktop, iOS.
- Existing uncommitted changes in the working tree (MainActivity, AdsManager, SettingsProgressRepository, CognitiveAnalyticsScreen, GymScreen) must not be clobbered.

## Left to the builder
File schema details, loader/DI wiring, how to word claims so they match what the sources actually say, which existing claims survive verification, and migration order.
