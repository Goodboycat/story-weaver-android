# Story Weaver — Android TTRPG Companion

Native Android Kotlin application built with **Jetpack Compose**, **Material Design 3**, **Firebase Auth (Google Credential Manager)**, **Cloud Firestore**, and **Firebase AI Logic (Gemini)**.

---

## Key Features

- **Google Sign-In Authentication**: One-tap silent & interactive Google Sign-In powered by Jetpack Credential Manager (`GetSignInWithGoogleOption`).
- **Real-Time Campaign Sync**: Create campaigns as a Game Master (with shareable Join Codes) or join campaigns as a Player.
- **Role-Based Workspace**:
  - **Game Master (10 Tabs)**: Step Guide, Players, NPCs, Monsters, Interactive Tactical Grid Map, Locations, Powers, Notes, Party Chat & Multiplayer Voice Room, Activity Logs.
  - **Player View (4 Tabs)**: Players (edit own sheet), Map (move own token), Chat & Voice Room, Logs.
- **Multiplayer Voice Chat**: Built into the **Chat** tab via a dedicated campaign **Jitsi Audio Room** (`aistudio_rpg_<storyId>`) with one-tap Join, Copy Invite Link, and Ping Party controls.
- **Session Schedule Notifier & Play-Day Reminders**: Game Masters can schedule the next session date & time; party members receive an immediate schedule notification (showing the play day and hour) plus an automatic play-day reminder notification.
- **Floating Tap-Based Dice Roller**: Build `XdY + modifiers`, roll live, and broadcast results directly into party chat.
- **AI Parsers**: Import character rosters, world items, and 12-slot body equipment from raw text via Firebase AI Logic.
- **Customization**: 5 theme presets, custom accent swatches, 4 text-size scales, and 12-language localization.

---

## Project Identifiers

- **App Name**: `Story Weaver`
- **Application ID**: `com.aistudio.storyweaver.kmrxpz`
- **Package Namespace**: `com.example`
- **Min / Target / Compile SDK**: `24` / `36` / `36`

---

## Setup Guide: Google Authentication, Firebase & App Check

Follow these steps if you clone this repository or connect it to your own Firebase project:

### 1. Create a Firebase Project & Enable Google Sign-In
1. Open the [Firebase Console](https://console.firebase.google.com/) and create or select your project.
2. Go to **Build → Authentication → Get Started**.
3. Open the **Sign-in method** tab, select **Google**, enable it, choose a project support email, and click **Save**.

### 2. Get Your Android SHA-1 and SHA-256 Fingerprints
Google Sign-In via Credential Manager requires your app's signing certificate fingerprints in Firebase.

Run this command in the project root to inspect `debug.keystore` (or decode `debug.keystore.base64` first with `base64 -d debug.keystore.base64 > debug.keystore`):

```bash
keytool -list -v \
  -keystore debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

Copy the **SHA1** and **SHA256** values. (For Play Store release builds, also add your Play App Signing SHA-1/SHA-256 from Google Play Console.)

### 3. Register the Android App & Download `google-services.json`
1. In Firebase Console → **Project Settings → Your apps → Add app → Android**.
2. Enter Android package name: `com.aistudio.storyweaver.kmrxpz`.
3. Paste your **SHA-1** certificate fingerprint (and add **SHA-256** in Project Settings afterward).
4. Download **`google-services.json`** and place it at:
   ```text
   app/google-services.json
   ```
5. Verify that `google-services.json` contains an `oauth_client` entry with `"client_type": 3` (Web Client ID). The Google Services Gradle plugin automatically generates `R.string.default_web_client_id` from this entry—do **not** hardcode it manually in `strings.xml`.

### 4. Configure Cloud Firestore & Security Rules
1. In Firebase Console → **Build → Firestore Database**, ensure your database exists (named or `(default)`).
2. Update `firestore_database_id` in `app/src/main/res/values/firebase_applet_config.xml` to match your Firestore database ID (use `(default)` if using the default database).
3. Deploy the included security rules (`firestore.rules`) using Firebase CLI:
   ```bash
   firebase deploy --only firestore:rules
   ```

### 5. Configure Firebase App Check (Required for AI Parsers)
Firebase AI Logic (`Firebase.ai`) requires App Check:
1. In Firebase Console → **Build → App Check**, register your Android app.
2. For **Debug / Emulator builds**, register a Debug token in App Check → **Manage debug tokens** and pass it via launch Intent extra `FIREBASE_APPCHECK_DEBUG_TOKEN` (handled automatically in `AppCheckHelper.kt`).
3. For **Release builds**, configure **reCAPTCHA Enterprise** (or Play Integrity) and put your site key in `app/src/main/res/values/firebase_applet_config.xml` under `recaptcha_site_key`.

---

## Automatic APK Builds on Push to GitHub

This repository includes `.github/workflows/android-debug-apk.yml`, which automatically builds a downloadable Debug APK on every `git push` (and manual **Run workflow** dispatch).

### Setup Steps for GitHub Actions
1. Push this project to your GitHub repository:
   ```bash
   git init
   git add .
   git commit -m "Initial Story Weaver Android project"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
2. **If `app/google-services.json` is not committed to Git**, encode it to base64 and add it as a GitHub Repository Secret named `GOOGLE_SERVICES_JSON_BASE64`:
   ```bash
   base64 -w 0 app/google-services.json
   ```
   Add the output in **GitHub Repo → Settings → Secrets and variables → Actions → New repository secret**.
3. Open the **Actions** tab in GitHub, select the latest **Build Android Debug APK** run, and download **`story-weaver-debug-apk`** from **Artifacts**.

---

## Local Build & Tests

> **Note:** This workspace uses system `gradle` (`9.3.1`) with JDK 17+.

```bash
# Build Debug APK
gradle assembleDebug

# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Run Firestore security rules tests (with Firestore emulator on 127.0.0.1:8085)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
