# Google Play Store CI/CD Pipeline Specification

## Goal
Establish a robust, automated CI/CD pipeline using GitHub Actions to build, sign, package, and publish the Android app (`studio.gameloop.tictactoe`) to the Google Play Store's **Internal Testing** track.

## Context & Key Constraint
- **Google Play API Restriction**: The Google Play Developer API requires that the app is created in the Google Play Console and the very first App Bundle (`.aab`) is uploaded manually via the web console before automated API publishing works.
- **Repository**: Hosted on GitHub (`https://github.com/Ashrafkhan19/tic-tac-toe.git`).
- **Project Structure**: Kotlin Multiplatform project using Gradle 9.5.1 and Android Gradle Plugin 9.1.1 (Target SDK 37, Min SDK 24, ApplicationId `studio.gameloop.tictactoe`).

---

## Architecture & Workflow Design

### 1. Two-Phase Rollout
- **Phase 1 (One-Time Prerequisites & Bootstrap)**:
  - Generate release upload keystore (`tictactoe-upload-key.jks`).
  - Configure release signing in `app/androidApp/build.gradle.kts`.
  - Create application `studio.gameloop.tictactoe` in Google Play Console.
  - Build initial signed `.aab` (locally or via GitHub Actions artifact download) and upload manually to the Internal Testing track.
  - Create a Google Cloud Service Account with Play Developer API access and invite it with "Releases" permission in Google Play Console.
  - Store required secrets in GitHub repository settings.
- **Phase 2 (Automated CI/CD Pipeline)**:
  - Runs in GitHub Actions whenever a git release tag (`v*`, e.g. `v1.0.0`) is pushed, or when manually triggered via `workflow_dispatch`.
  - Builds the release bundle (`.aab`), signs it with the decoded keystore, archives artifacts in GitHub run summary, and uploads directly to Google Play's `internal` track.

---

## Detailed Components

### Component 1: Gradle Signing & Versioning Configuration
**File**: `app/androidApp/build.gradle.kts`
- Add support for release signing configuration:
  - Checks for environment variables:
    - `KEYSTORE_PATH`: Path to `.jks` file on runner or local disk.
    - `KEYSTORE_PASSWORD`: Keystore password.
    - `KEY_ALIAS`: Key alias.
    - `KEY_PASSWORD`: Key password.
  - If credentials are present, configures `signingConfigs.getByName("release")` and links it to `buildTypes.release.signingConfig`.
  - If not present (e.g. standard local debug build without env vars), it skips signing without breaking local builds.
- Dynamic version support:
  - Reads `project.findProperty("versionCode")` if provided (defaults to existing `versionCode = 1`).
  - Reads `project.findProperty("versionName")` if provided (defaults to existing `versionName = "1.0"`).

### Component 2: GitHub Actions Workflow
**File**: `.github/workflows/deploy-play-store.yml`
- **Triggers**:
  - `push: tags: ['v*']`
  - `workflow_dispatch`:
    - `inputs.track`: Track selection (default `internal`).
    - `inputs.version_name`: Custom version name override (optional).
- **Execution Environment**:
  - Runner: `ubuntu-latest`.
  - Java: JDK 21 (Temurin) via `actions/setup-java@v4` with built-in Gradle caching.
  - Android SDK: Setup with `android-actions/setup-android@v3`.
- **Pipeline Steps**:
  1. `actions/checkout@v4` with full fetch or tag checkout.
  2. Setup Java 21 & Gradle cache.
  3. Decode base64 keystore from `secrets.PLAY_STORE_KEYSTORE_BASE64` to `$RUNNER_TEMP/keystore.jks`.
  4. Determine `versionCode` (using GitHub Run Number e.g. `$GITHUB_RUN_NUMBER`) and `versionName` (from git tag or input).
  5. Build Release App Bundle via `./gradlew :app:androidApp:bundleRelease` passing version properties and signing env variables.
  6. Archive `.aab` file and ProGuard mapping file (`mapping.txt`) using `actions/upload-artifact@v4` (retained for 30 days).
  7. Deploy to Google Play Store using `r0adkll/upload-google-play@v1`:
     - Service Account: Decoded/passed from `secrets.PLAY_SERVICE_ACCOUNT_JSON`.
     - Package Name: `studio.gameloop.tictactoe`.
     - Track: `internal` (or dynamic track).
     - Release status: `completed`.

### Component 3: Developer Setup Guide & Documentation
**File**: `docs/PLAY_STORE_SETUP.md`
- Step-by-step documentation detailing:
  1. Keystore creation with `keytool`.
  2. Converting the keystore file to base64 (`base64 -i tictactoe-upload-key.jks | pbcopy`).
  3. Setting up Google Cloud Project and Google Play Developer API Service Account.
  4. Granting permissions in Google Play Console (Users & Permissions -> Invite user).
  5. Configuring all 5 GitHub Secrets:
     - `PLAY_STORE_KEYSTORE_BASE64`
     - `KEYSTORE_PASSWORD`
     - `KEY_ALIAS`
     - `KEY_PASSWORD`
     - `PLAY_SERVICE_ACCOUNT_JSON`
  6. Instructions for the initial manual upload before triggering the automated pipeline.

---

## Verification & Testing
- Validate Gradle syntax by executing a dry-run / build check.
- Validate workflow YAML syntax using a YAML parser / linter.
- Verify that Gradle builds without errors when signing variables are omitted.
- Verify that Gradle accepts signing variables and compiles properly when signing variables are set.

