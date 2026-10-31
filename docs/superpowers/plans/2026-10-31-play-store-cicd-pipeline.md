# Google Play Store CI/CD Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build, sign, package, and publish the Android app (`studio.gameloop.tictactoe`) to Google Play Store's Internal Testing track using GitHub Actions on git release tags and manual triggers.

**Architecture:** A GitHub Actions workflow configured with JDK 21 that unpacks a base64-encoded release upload keystore, dynamically sets the version code/name from git context, builds an Android App Bundle (`.aab`) with Gradle, archives artifacts, and publishes to Google Play Console via the Play Developer API using a Google Cloud service account.

**Tech Stack:** GitHub Actions, Android Gradle Plugin 9.1.1, Kotlin Multiplatform, Gradle 9.5.1, `r0adkll/upload-google-play@v1`, Java JDK 21.

**Spec:** `docs/superpowers/specs/2026-10-31-play-store-cicd-pipeline.md`

## Global Constraints
- Target SDK is 37, Min SDK is 24, ApplicationId is `studio.gameloop.tictactoe`.
- Do not hardcode signing credentials or passwords in any repository files.
- Local debug and test builds must continue to function normally when signing environment variables are unset.
- Target the `internal` testing track on Google Play.

## Review Focus
1. Missing signing environment variables during local development: must fall back to unsigned release or debug default without failing the build.
2. Keystore file path handling: must handle file paths with spaces or special characters on Linux runners cleanly.
3. Version code collisions: each run must increment or supply a valid integer `versionCode` greater than previous releases.
4. Initial manual upload caveat: clear documentation for the developer so they do not get an API rejection on the first deployment.
5. GitHub Actions secret naming consistency: names in the workflow must match the documentation exactly.

---

### Task 1: Configure Release Signing & Dynamic Versioning in Gradle

**Files:**
- Modify: `app/androidApp/build.gradle.kts`

**Context:**
Currently, `app/androidApp/build.gradle.kts` has hardcoded `versionCode = 1` and `versionName = "1.0"`, and `buildTypes.release` does not specify a `signingConfig`. We need to read credentials from environment variables (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and allow dynamic versioning via Gradle properties (`versionCode`, `versionName`).

- [ ] **Step 1.1**: Open `app/androidApp/build.gradle.kts` and add signing configuration and project property versioning logic:
  - Read `val envKeystorePath = System.getenv("KEYSTORE_PATH")`
  - If `envKeystorePath != null && file(envKeystorePath).exists()`, create a `release` signing config using `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
  - Set `buildTypes.getByName("release").signingConfig` to this release signing config when credentials are present.
  - Dynamically read `versionCode` from project property `versionCode` (converted to int) falling back to `1`.
  - Dynamically read `versionName` from project property `versionName` falling back to `"1.0"`.
- [ ] **Step 1.2**: Verify syntax and build configuration without environment variables:
  - Check that the gradle build files evaluate cleanly without error.
- [ ] **Step 1.3**: Commit the changes:
  - `git commit -m "build(android): configure release signing and dynamic versioning"`

---

### Task 2: Create GitHub Actions Deployment Workflow

**Files:**
- Create: `.github/workflows/deploy-play-store.yml`

**Context:**
The GitHub Actions workflow triggers on tag pushes (`v*`) and manual workflow dispatch. It handles environment setup, secret decoding, building the release AAB, artifact archiving, and automated publishing via `r0adkll/upload-google-play@v1`.

- [x] **Step 2.1**: Create `.github/workflows/deploy-play-store.yml` with the following structure:
  - Name: `Deploy to Google Play`
  - On:
    - `push: tags: ['v*']`
    - `workflow_dispatch: inputs: track, version_code, version_name`
  - Job `build-and-deploy`:
    - Runs on `ubuntu-latest`
    - Steps:
      - Checkout repository (`actions/checkout@v4`)
      - Setup JDK 21 (`actions/setup-java@v4`, `distribution: 'temurin'`, `cache: 'gradle'`)
      - Setup Android SDK (`android-actions/setup-android@v3`)
      - Grant execution permission to `gradlew`
      - Decode base64 keystore to `$RUNNER_TEMP/keystore.jks`
      - Calculate version code (using `github.run_number + 100` or custom input) and version name (from tag e.g. `${GITHUB_REF_NAME#v}` or input)
      - Execute `./gradlew :app:androidApp:bundleRelease -PversionCode=... -PversionName=...` with signing environment variables
      - Archive `.aab` file (`app/androidApp/build/outputs/bundle/release/*.aab`) and mapping file via `actions/upload-artifact@v4`
      - Decode service account JSON to `$RUNNER_TEMP/service_account.json`
      - Deploy to Google Play using `r0adkll/upload-google-play@v1` targeting `track: internal`
- [x] **Step 2.2**: Validate YAML syntax:
  - Verify that the workflow file is well-formed YAML and uses valid action versions.
- [x] **Step 2.3**: Commit the changes:
  - `git commit -m "ci: add GitHub Actions workflow to publish app to Google Play Store"`

---

### Task 3: Create Setup Guide & Secrets Documentation

**Files:**
- Create: `docs/PLAY_STORE_SETUP.md`

**Context:**
The developer needs a clear, fool-proof guide to set up the prerequisites: creating the keystore, base64 encoding it, creating the app on Play Console, performing the initial manual upload, creating the Google Cloud Service Account, and configuring GitHub Secrets.

- [ ] **Step 3.1**: Create `docs/PLAY_STORE_SETUP.md` including:
  - **Prerequisites Overview & Google Play Console First Upload Rule**.
  - **Step 1: Generating the Upload Keystore**:
    - Complete `keytool` command with recommended parameters (RSA 2048, 10000 days validity).
  - **Step 2: Base64 Encoding the Keystore**:
    - macOS and Linux commands (`base64 -i ... | pbcopy` or `base64 -w 0 ...`).
  - **Step 3: Creating the App on Google Play Console**:
    - App details, package name `studio.gameloop.tictactoe`, default language.
  - **Step 4: Initial Manual Bundle Upload**:
    - How to build the bundle locally or download from the first GitHub Actions run.
    - Uploading to **Internal Testing** track in Google Play Console.
  - **Step 5: Google Play Developer API & Service Account Setup**:
    - Enabling Google Play Android Developer API in Google Cloud Console.
    - Creating Service Account and generating JSON Key.
    - Inviting Service Account in Google Play Console (Users & Permissions -> Releases permission).
  - **Step 6: GitHub Repository Secrets Checklist**:
    - `PLAY_STORE_KEYSTORE_BASE64`
    - `KEYSTORE_PASSWORD`
    - `KEY_ALIAS`
    - `KEY_PASSWORD`
    - `PLAY_SERVICE_ACCOUNT_JSON`
  - **Step 7: How to Trigger a Release**:
    - Pushing a git tag: `git tag v1.0.1 && git push origin v1.0.1`.
    - Or using the "Run workflow" button in the GitHub Actions tab.
- [ ] **Step 3.2**: Commit the changes:
  - `git commit -m "docs: add Google Play Store CI/CD setup guide and secrets checklist"`

---

### Task 4: Verification and Final Sanity Checks

**Files:**
- Review: `app/androidApp/build.gradle.kts`
- Review: `.github/workflows/deploy-play-store.yml`
- Review: `docs/PLAY_STORE_SETUP.md`

- [ ] **Step 4.1**: Verify Gradle syntax and build consistency with dummy environment variables to ensure signing logic evaluates cleanly.
- [ ] **Step 4.2**: Verify `.gitignore` ensures keystores (`*.jks`, `*.keystore`) and JSON keys are ignored so credentials are never accidentally committed.
- [ ] **Step 4.3**: Provide a final walkthrough and summary to the user.

