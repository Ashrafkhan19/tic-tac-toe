# Google Play Store CI/CD Setup Guide

This guide walks you through setting up automated releases for **Tic Tac Toe** (`studio.gameloop.tictactoe`) to the Google Play Store using GitHub Actions.

---

## The Golden Rule of Google Play Developer API

> **IMPORTANT:** Google Play Developer API **does not permit** automated tools to upload the very first build of a new application.
>
> You **MUST manually upload the initial `.aab` file once** through the [Google Play Console](https://play.google.com/console) web interface before the automated GitHub Actions pipeline can deploy subsequent versions. Attempting to deploy via CI/CD before this manual upload will result in an API error:
> `Google Play API: Package not found / Must upload an APK or Android App Bundle at least once.`

---

## Step 1: Generate the Upload Keystore

Android requires all app bundles to be digitally signed before they can be installed or accepted by Google Play.

1. Open your terminal and run `keytool` (included with Java/JDK):

   ```bash
   keytool -genkey -v \
     -keystore tictactoe-upload-key.jks \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -alias tictactoe
   ```

2. When prompted:
   - **Keystore password:** Choose a strong password and save it in a password manager.
   - **Certificate info:** Enter your details (First/Last Name, Organization, Country Code).
   - **Key password:** Enter the key password (or press Enter if using PKCS12 / same password).

> **SECURITY WARNING:**
> - **Never** commit `.jks`, `.keystore`, or key files to version control.
> - Verify that `*.jks` and `*.keystore` are included in your `.gitignore`:
>   ```gitignore
>   *.jks
>   *.keystore
>   ```
> - **Back up this keystore file and its password safely!** If you lose this keystore, you will not be able to publish updates to Google Play unless you request a key reset through Google Play Support.

---

## Step 2: Base64-Encode the Keystore for GitHub Secrets

GitHub Actions cannot store binary files directly in repository secrets, so the `.jks` file must be encoded as a Base64 string.

### On macOS
```bash
base64 -i tictactoe-upload-key.jks | pbcopy
```
*(This copies the Base64 string directly to your clipboard.)*

### On Linux
```bash
base64 -w 0 tictactoe-upload-key.jks
```
*(Copy the printed output to your clipboard.)*

### On Windows (PowerShell)
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("tictactoe-upload-key.jks")) | Set-Clipboard
```

---

## Step 3: Create the Application in Google Play Console

1. Log in to the [Google Play Console](https://play.google.com/console).
2. Click **Create app** (top right corner).
3. Fill in the initial application details:
   - **App name:** `Tic Tac Toe` (or `GameLoop TicTacToe`)
   - **Default language:** English (United States) - `en-US`
   - **App or game:** `Game`
   - **Free or paid:** `Free`
4. Accept the **Declarations** (Developer Program Policies, US export laws) and click **Create app**.
5. The application package name is configured in the project as:
   ```
   studio.gameloop.tictactoe
   ```
6. Complete the mandatory **App Content** questionnaires under **Policy and programs -> App content** (Privacy policy, Ads, Content rating, Target audience, Data safety).

---

## Step 4: Build and Manually Upload the Initial Bundle

Build the signed release bundle locally on your machine to satisfy the "Golden Rule" requirement.

### 1. Build the Release Bundle Locally

From the root of the project, run:

```bash
KEYSTORE_PATH="$PWD/tictactoe-upload-key.jks" \
KEYSTORE_PASSWORD="<your-keystore-password>" \
KEY_ALIAS="tictactoe" \
KEY_PASSWORD="<your-key-password>" \
./gradlew :app:androidApp:bundleRelease
```

> **Note:** Gradle resolves both absolute paths (such as `$PWD/tictactoe-upload-key.jks`) and root-project-relative paths automatically.

The resulting bundle will be located at:
```
app/androidApp/build/outputs/bundle/release/androidApp-release.aab
```

### 2. Upload to Google Play Console

1. In the Google Play Console, navigate to your app dashboard.
2. In the left menu under **Testing**, click **Internal testing**.
3. Click **Create new release** (top right).
4. If prompted with *Play App Signing*, click **Choose signing key** and choose **Use Google-generated key** (recommended) or use your upload key.
5. In the **App bundles** section, upload `androidApp-release.aab`.
6. Enter a release name (e.g., `1.0.0 (1)`) and optional release notes.
7. Click **Next**, review any warnings, and click **Save and publish** to the Internal testing track.

Once this initial manual release is saved in the console, your application is activated for automated Google Play Developer API uploads.

---

## Step 5: Google Play Developer API & Service Account Setup

To allow GitHub Actions to deploy releases automatically, you must create a Google Cloud Service Account and grant it access in the Google Play Console.

### 1. Link Google Play Console to Google Cloud Project
1. In the Google Play Console, navigate to the developer account home page.
2. Go to **Setup** -> **API access**.
3. Choose to link an existing Google Cloud project or click **Create new Google Cloud project**.

### 2. Create the Service Account in Google Cloud Console
1. On the **API access** page in Google Play Console, click **View in Google Cloud Platform** (or visit [Google Cloud IAM Console](https://console.cloud.google.com/iam-admin/serviceaccounts)).
2. Click **+ Create Service Account**.
3. Enter the service account details:
   - **Service account name:** `play-store-deployer`
   - **Service account ID:** `play-store-deployer`
   - **Description:** `CI/CD automated deployment to Google Play`
4. Click **Create and Continue**.
5. In **Grant this service account access to project**, you may assign the role **Service Account User** (or leave empty if solely managing Play Console).
6. Click **Done**.

### 3. Generate the Service Account JSON Key
1. In the Google Cloud Console, click on the newly created service account (`play-store-deployer@...`).
2. Navigate to the **Keys** tab.
3. Click **Add Key** -> **Create new key**.
4. Select **JSON** and click **Create**.
5. Save the downloaded `.json` file securely (e.g. `service-account-key.json`).

> **Recommended:** Store downloaded credentials in a secure folder outside the workspace (such as `~/.credentials/`) to prevent accidental staging.

### 4. Enable the Google Play Android Developer API
1. In Google Cloud Console, go to **APIs & Services** -> **Library**.
2. Search for **Google Play Android Developer API**.
3. Click **Enable**.

### 5. Grant Permissions in Google Play Console
1. Return to the **Google Play Console** -> **Setup** -> **API access**.
2. The newly created service account should be listed under **Service accounts**.
3. Click **Grant access** (or go to **Users and permissions** -> **Invite new users**).
4. Enter the service account email.
5. Under **App permissions**, click **Add app** and select `studio.gameloop.tictactoe`.
6. Under **Account permissions** / **App permissions**, ensure the following permissions are checked:
   - **Releases:**
     - *Manage release tracks* (Production, Open testing, Closed testing, Internal testing)
     - *Edit and draft releases*
     - *Manage testing tracks and edit tester lists*
7. Click **Invite user** (or **Save changes**).

---

## Step 6: GitHub Repository Secrets Checklist

Go to your repository on GitHub:
**Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**.

Add the following 5 secrets:

| Secret Name | Description | Example / Format |
|---|---|---|
| `PLAY_STORE_KEYSTORE_BASE64` | Base64-encoded string of `tictactoe-upload-key.jks` | `MIIKvgIBAzCCCn8GCSqGSIb3DQEHAaCCCnAEgg...` (from Step 2) |
| `KEYSTORE_PASSWORD` | Password used when creating the keystore | Plain text password |
| `KEY_ALIAS` | Key alias defined in Step 1 | `tictactoe` |
| `KEY_PASSWORD` | Password for the key alias | Plain text password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Entire content of the Google Cloud Service Account JSON key | Raw JSON string (`{"type": "service_account", ...}`) or its Base64 encoding |

---

## Step 7: Releasing New Versions via CI/CD

Once the setup is complete, you can release updates automatically using either of the following methods:

### Method A: Git Tag (Recommended)

Pushing any tag starting with `v` (e.g., `v1.0.1`, `v1.1.0`) automatically triggers the pipeline:

```bash
# Ensure your branch is up-to-date
git checkout main
git pull origin main

# Create and push a semver tag
git tag v1.0.1
git push origin v1.0.1
```

**How versioning works on tag push:**
- `versionName` is extracted from the tag (e.g., `v1.0.1` -> `1.0.1`).
- `versionCode` is computed automatically from GitHub Actions run number: `GITHUB_RUN_NUMBER + 100` (guaranteeing strictly increasing numbers).
- The release is published to the **internal** track by default.

### Method B: Manual Trigger (GitHub UI)

1. In your GitHub repository, open the **Actions** tab.
2. In the left sidebar, select the **Deploy to Google Play** workflow.
3. Click **Run workflow** (top right dropdown).
4. Configure optional inputs:
   - **Google Play track:** `internal` (default), `alpha`, `beta`, or `production`.
   - **Custom versionCode override:** (e.g., `105`, or leave blank for auto).
   - **Custom versionName override:** (e.g., `1.0.2`, or leave blank for default).
5. Click **Run workflow**.

---

## Troubleshooting & Common Pitfalls

### 1. `Google Play API: Package not found` or `Must upload bundle at least once`
* **Cause:** The initial `.aab` file was not manually uploaded to Google Play Console web interface.
* **Fix:** Follow [Step 4](#step-4-build-and-manually-upload-the-initial-bundle) to build and manually upload the initial bundle via Google Play Console web UI under **Internal testing**.

### 2. `Keystore was tampered with, or password was incorrect`
* **Cause:** The `KEYSTORE_PASSWORD` or `KEY_PASSWORD` secrets do not match the keystore, or `PLAY_STORE_KEYSTORE_BASE64` was corrupted during copy-paste.
* **Fix:** Re-encode the keystore using `base64 -i tictactoe-upload-key.jks | pbcopy` and verify that no trailing whitespace or newlines were added.

### 3. `Version code XXX has already been used`
* **Cause:** Google Play requires each uploaded build to have a strictly higher `versionCode` than all previous uploads.
* **Fix:** When running the workflow manually, supply a higher integer in `Custom versionCode override` (e.g. `200`). When deploying via tags, ensure GitHub run numbers haven't reset.

### 4. `The caller does not have permission` / `403 Forbidden`
* **Cause:** The Google Cloud Service Account has not been invited in Google Play Console or lacks release permissions.
* **Fix:** Check **Google Play Console -> Users and permissions**. Verify the service account email is listed with **Manage release tracks** permissions for `studio.gameloop.tictactoe`. Changes can take 5–15 minutes to propagate across Google's servers.

### 5. `Google Play Android Developer API has not been used in project...`
* **Cause:** The API is disabled in your linked Google Cloud project.
* **Fix:** Visit [Google Cloud Console API Library](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com) and click **Enable**.

---

## Summary Checklist

- [ ] Upload keystore generated (`tictactoe-upload-key.jks`) and backed up safely.
- [ ] Keystore files excluded in `.gitignore`.
- [ ] Application created in Google Play Console with package name `studio.gameloop.tictactoe`.
- [ ] Initial release bundle built locally and uploaded manually to **Internal testing**.
- [ ] Google Play Android Developer API enabled in Google Cloud Console.
- [ ] Service Account created and JSON key downloaded.
- [ ] Service Account invited to Google Play Console with release permissions.
- [ ] All 5 secrets added to GitHub repository (**Settings -> Secrets and variables -> Actions**).
- [ ] Trigger deployment using `git tag v1.0.0` or via GitHub Actions manual dispatch!
