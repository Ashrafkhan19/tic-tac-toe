# Tic Tac Toe v1: retention and growth features

Status: draft for approval. The project is not a git repository, so this file is not committed.

## Intent and why

The app is a working Kotlin Multiplatform tic-tac-toe game (Android, iOS, desktop; Compose Multiplatform UI) with a 3x3 board, vs-computer and 2-player modes, Easy and Unbeatable AI, a session scoreboard, and a phone/tablet adaptive layout. It has nothing that brings a player back: no saved data, no goals, no reason to open it tomorrow.

The owner wants more downloads and more daily returns. Plain tic-tac-toe is solved, so it gets boring after about 10 games. v1 adds a daily habit loop, progression, and sharing on top of the existing game, ships quickly, and measures whether people return before anything bigger is built.

## Goals

1. A player has a reason to open the app every day (daily challenge plus streak).
2. Progress persists between launches (stats, streak, XP, unlocks).
3. Every result can be shared in one tap (drives downloads).
4. Retention is measured (D1, D7, D30) from the first release.
5. The game earns money without hurting retention.

## Non-goals (v2, only if v1 numbers justify it)

Online multiplayer, friend matches, leaderboards, Ultimate Tic-Tac-Toe, 4x4/5x5 boards, accounts, any custom backend.

## Anti-goals

- No interstitial ads after games. Ads are rewarded and opt-in only.
- No mandatory signup before playing.
- No guilt-trip or spammy notifications. Maximum one reminder per day.
- Nothing may make the first launch slower to reach a playable board than it is today.

## Constraints

- Build on the existing code: modules `core` (rules, AI, no UI) and `app/shared` (MVI presentation and Compose UI), use cases plus the `AppContainer` composition root. New logic goes in `core` when it is pure rules, and in `app/shared` when it is presentation.
- Targets: Android and iOS phones and tablets are the priority. Desktop must keep compiling and running but gets no special work.
- No backend in v1. Everything works offline.
- Platform services (storage, notifications, share sheet, ads, purchases, analytics) go behind interfaces in common code with per-platform implementations, plus a fake implementation so the game and tests run without any SDK or account.
- Existing tests must keep passing; new rules code gets tests.

## v1 features

### 1. Saved progress
Persist across launches: lifetime stats (games, wins, losses, draws per difficulty), current and best streak, last daily-challenge date and result, XP, unlocked and selected theme, settings, and whether ads are removed. The current scoreboard is session-only and stays that way.

### 2. Daily challenge and streak
- One challenge per calendar day in the device's local time, the same for every player, with no backend. It is derived deterministically from the date (seeded generation).
- My call, shape of the challenge: a pre-played board where the player is X to move and must find the forced win. The generator picks positions that really have a forced win, verified with the existing minimax logic. Difficulty ramps through the week (easy early in the week, hardest at the weekend).
- Completing the day's challenge extends the streak and awards bonus XP. Failing lets the player retry the same day.
- My call, streak freeze: one freeze is earned for every 7-day streak (max 1 banked). A missed day consumes it instead of resetting the streak. Freezes can also be earned by watching a rewarded ad.
- A home screen shows the streak, today's challenge status, and the entry points to free play (vs Computer, 2 Players).
- My call: the clock is the device clock, and a user changing it is not defended against in v1.

### 3. XP, levels, themes
- XP for every finished game, with bonuses for beating a harder difficulty and for the daily challenge. Levels derive from XP.
- Unlockable themes (colors for board, marks, background) at certain levels. The existing theme is the default and is always available.
- My call, a new Medium difficulty between Easy and Unbeatable, so wins feel possible. Unbeatable remains, and beating Medium is a goal.

### 4. Share and remind
- Share card after a daily challenge or a win: an image or emoji-grid text plus a store link, via the platform share sheet.
- One local (on-device) notification a day, at a time the player can set, about the streak at risk or the new challenge. Off if the player declines permission. No push server.

### 5. Money
- Rewarded ads only (for a streak freeze or a hint on the daily challenge).
- One non-consumable in-app purchase: remove ads and unlock all themes.

### Measurement
Log events (app open, game start/finish with mode and difficulty, challenge start/finish, streak length, share tapped, ad watched, purchase) to a mobile analytics service. D1, D7 and D30 retention are read from that service's dashboards. No personal data beyond what the analytics SDK collects by default, and the store privacy declarations must match.

## What the owner must supply (needed before step 5 and parts of 4 and measurement ship)

Apple Developer and Google Play developer accounts, an ad network account and ad unit IDs, in-app product IDs, and an analytics project and its config files. Until they exist the fake implementations are used and the features are developed against them.

## Left to the builder

Persistence library, the exact seeded-generation algorithm, screen layouts and navigation, XP and level curves, theme palettes, notification default time, event naming, and the choice of ad, purchase, and analytics SDKs (any that work in a Kotlin Multiplatform project).

## Suggested order

1 saved progress, 2 daily challenge and streak (with home screen), 3 XP, themes, Medium difficulty, 4 share and local notification, then analytics, then ads and purchase. Each step leaves the app working and shippable.

## Rules settled after the builder check (all my calls unless marked)

- Medium AI: the existing minimax with a fixed chance (about 25%) of playing a random legal move instead. Persisted stats use the stable difficulty keys Easy, Medium, Unbeatable from step 1.
- Streak: counts only days on which the daily challenge was completed. "Completed" means winning the pre-played game (any number of moves). A draw or loss can be retried without limit that day. The generator outputs just the position.
- Streak freeze: covers exactly one missed day. A gap of two or more days breaks the streak even with a freeze banked.
- Dates: stored as local calendar dates (yyyy-MM-dd). A timezone change just changes today's date. A date earlier than the last recorded one is ignored, so the clock moving backwards never lowers or double-counts a streak.
- What counts: only vs-Computer games count towards lifetime stats and XP. 2-player games and abandoned games do not. There is no existing saved data to migrate, because the current scoreboard is session-only.
- Share card: text with an emoji grid in v1 (no bitmap rendering). The store link is a configurable placeholder until the apps are published.
- Themes: unlock levels are fixed in code. The "remove ads and unlock all themes" purchase is restorable through the store's restore-purchases flow.
- Desktop: ads, purchases, and analytics are no-ops.

## Success

After release, D1 retention, D7 retention, and the share rate are visible in analytics, so v2 scope can be chosen from data.
