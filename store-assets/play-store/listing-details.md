# Google Play Store Listing Details & Store Assets

This document contains all official store listing copy and asset references required to publish the application on Google Play Console for `studio.gameloop.tictactoe`.

---

## 1. App Identity & Copy

### App Title
```text
Tic Tac Toe: Aurora Glow
```
*(24 / 30 characters maximum)*

### Short Description
```text
Experience classic Tic Tac Toe reimagined with vibrant neon glow and smooth PvP!
```
*(79 / 80 characters maximum)*

### Full Description
```markdown
Step into the luminous world of **Tic Tac Toe: Aurora Glow** — the classic 3x3 strategy board game reimagined with a breathtaking cyberpunk neon aesthetic, silky smooth animations, and instantaneous offline play!

Whether you're looking for a quick brain teaser on your daily commute or a competitive duel against friends on the same device, Aurora Glow delivers pure, uncluttered strategy gaming at its finest.

✨ KEY FEATURES:

🌟 VIBRANT NEON & AURORA AESTHETIC
Immerse yourself in deep cosmic midnight visuals contrasted with radiant neon teal (X) and luminous violet (O) glowing marks. Designed to look gorgeous on modern OLED and AMOLED screens while being gentle on your battery.

👥 LOCAL PASS & PLAY 2-PLAYER MODE
Hand the phone back and forth for thrilling tabletop battles with friends, family, or colleagues anytime, anywhere.

⚡ SILKY SMOOTH 60+ FPS PERFORMANCE
Built natively with Jetpack Compose for Android. Zero lag, lightning-fast tap responsiveness, and fluid visual effects.

📊 REAL-TIME SCOREBOARD & WIN STREAKS
Track your performance across multiple rounds with an automatic scorekeeper recording Player X wins, Player O wins, tied draws, and ongoing win streaks. Reset your stats whenever you wish to start fresh.

🎉 SPECTACULAR CELEBRATION FX
Enjoy dynamic neon win lines and colorful ambient confetti celebrations whenever a victorious trio is aligned.

✈️ 100% OFFLINE PLAY
No internet? No problem! Play completely offline with zero data consumption, making it the perfect companion for flights, road trips, and commutes.

🛡️ ZERO CLUTTER, ZERO NONSENSE
Clean, minimalist interface with no unnecessary permissions or bloated menus. Tap to start playing in under a second!

---

HOW TO PLAY:
1. Two players take turns placing their glowing mark (X or O) on an empty grid tile.
2. The first player to align 3 consecutive marks horizontally, vertically, or diagonally wins the round!
3. If all 9 tiles are filled without a 3-in-a-row alignment, the round ends in a draw.

Download **Tic Tac Toe: Aurora Glow** today and illuminate your strategic mind!
```
*(~1,900 / 4,000 characters maximum)*

---

## 2. Store Assets Inventory

All graphic assets are located in `store-assets/play-store/`:

| Asset File | Google Play Requirement | Exact Dimensions | Status |
|---|---|---|---|
| `app-icon-512x512.png` | App Icon (32-bit PNG with alpha, max 1024 KB) | 512 × 512 px | Verified ✅ (83 KB) |
| `feature-graphic-1024x500.png` | Feature Graphic (JPG or 24/32-bit PNG, max 15 MB) | 1024 × 500 px | Verified ✅ (200 KB) |
| `screenshot-1-gameplay.png` | Phone Screenshot (9:16 aspect ratio, min 1080px) | 1080 × 1920 px | Verified ✅ (265 KB) |
| `screenshot-2-victory.png` | Phone Screenshot (9:16 aspect ratio, min 1080px) | 1080 × 1920 px | Verified ✅ (319 KB) |
| `screenshot-3-scoreboard.png` | Phone Screenshot (9:16 aspect ratio, min 1080px) | 1080 × 1920 px | Verified ✅ (240 KB) |
| `screenshot-4-features.png` | Phone Screenshot (9:16 aspect ratio, min 1080px) | 1080 × 1920 px | Verified ✅ (271 KB) |

---

## 3. In-App Launcher Icons

The Android application launcher icons have been compiled and updated in `app/androidApp/src/main/res/`:

- **Adaptive Vector Icon (Android 8.0 / API 26+):**
  - Background: `res/drawable/ic_launcher_background.xml` (Deep aurora gradient with ambient glow flares)
  - Foreground: `res/drawable/ic_launcher_foreground.xml` & `res/drawable-v24/ic_launcher_foreground.xml` (Glow-effect grid and neon marks safely bounded within the 72dp adaptive zone)
  - Definition: `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`
- **Density Mipmap Fallbacks (Legacy PNG):**
  - `res/mipmap-mdpi/` (48 × 48 px)
  - `res/mipmap-hdpi/` (72 × 72 px)
  - `res/mipmap-xhdpi/` (96 × 96 px)
  - `res/mipmap-xxhdpi/` (144 × 144 px)
  - `res/mipmap-xxxhdpi/` (192 × 192 px)

---

## 4. Google Play Console Configuration Checklist

When setting up the listing in Google Play Console:

1. **App Category & Tags:**
   - **Application Type:** Game
   - **Category:** Board / Casual / Puzzle
   - **Tags:** Board game, Tic-tac-toe, Offline, Casual, 2-player
2. **Contact Details:**
   - Developer Email: Enter your support email (e.g. `support@gameloop.studio` or developer email)
3. **Content Rating (IARC Questionnaire):**
   - Game contains violence: **No**
   - Game contains offensive language: **No**
   - Game shares user location or data: **No**
   - Result: **Everyone (PEGI 3 / ESRB Everyone)**
4. **Target Audience & Content:**
   - Target age group: 13 and older (or All Ages if accompanied by a Privacy Policy)
5. **App Access & Ads:**
   - Contains Ads: Select **No** (unless you integrate ad SDKs later)
   - App Access: All functionality is available without special access
