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
- **Application ID**: `com.aistudio.storyweaver.d66efx`
- **Package Namespace**: `com.example`
- **Min / Target / Compile SDK**: `24` / `36` / `36`

---

## Setup Guide: Google Authentication, Firebase & App Check

Follow these steps if you clone this repository or connect it to your own Firebase project:

### 1. Create a Firebase Project & Enable Google (Gmail) Sign-In
1. Open the [Firebase Console](https://console.firebase.google.com/) and create or select your project.
2. Go to **Build → Authentication → Get Started**.
3. Open the **Sign-in method** tab, click **Add new provider → Google**, toggle **Enable**, select a **Project support email** (your Gmail/Google Workspace address), and click **Save**.

### 2. Configure the Google Cloud OAuth Consent Screen (Required for Gmail Accounts)
Because Story Weaver signs users in with their Gmail/Google account via Jetpack Credential Manager (`GetSignInWithGoogleOption`), your Google Cloud project's OAuth consent screen must allow Gmail users:
1. Open the [Google Cloud Console](https://console.cloud.google.com/) and select the **same project** linked to Firebase.
2. Go to **Google Auth Platform** (or **APIs & Services → OAuth consent screen**).
3. Complete the **App Information** (App name: `Story Weaver`, User support email, Developer contact email) and choose **External** audience.
4. Under **Audience / Publishing status**:
   - **While in Testing mode**: Click **Add users** under **Test users** and add every Gmail address (yours and your players') that will sign in.
   - **For all Gmail users**: Click **Publish App** (and optionally submit verification if deploying publicly) so any Gmail account can sign in without `403: access_denied`.

### 3. Get Your Android SHA-1 and SHA-256 Fingerprints
Google Sign-In (`CredentialManager`) validates the signing certificate of the APK against Firebase. **The SHA-1 in Firebase must match the keystore used to sign the APK you install.**

Run this command in the project root to inspect `debug.keystore` (if `debug.keystore` does not exist locally, first run `base64 -d debug.keystore.base64 > debug.keystore`—this is the exact keystore used by GitHub Actions):

```bash
keytool -list -v \
  -keystore debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

Copy both **SHA1** and **SHA256**.
- **If building locally in Android Studio** on another machine, also inspect `~/.android/debug.keystore` (`-storepass android`) and add that machine's SHA-1 too.
- **For Google Play / Release APKs**, copy the **App signing key certificate SHA-1 & SHA-256** from Google Play Console → **Setup → App signing**.

### 4. Register the Android App & Download an Updated `google-services.json`
1. In Firebase Console → **Project Settings → Your apps → Add app → Android**.
2. Enter Android package name: `com.aistudio.storyweaver.d66efx`.
3. Paste your **SHA-1** certificate fingerprint (and add **SHA-256** under **Add fingerprint** in Project Settings).
4. **Re-download `google-services.json`** *after* enabling Google Sign-In and adding your SHA-1, then save it at:
   ```text
   app/google-services.json
   ```
5. Open `app/google-services.json` and confirm it contains an `oauth_client` entry with `"client_type": 3` (your **Web Client ID**). The Google Services Gradle plugin automatically generates `R.string.default_web_client_id` from that entry—never hardcode it manually in `strings.xml`.

### 5. Device & Emulator Requirements for Gmail Sign-In
- **Physical Android Phone**: Works out of the box once a Gmail account is signed in on the device.
- **Android Emulator**: Make sure your AVD uses a **Google Play** system image (not AOSP without Play Services) and sign into a Gmail account in **Android Settings → Passwords & accounts** (or follow the Google Sign-In prompt).

### Gmail Sign-In Troubleshooting
| Symptom / Error | Cause | Fix |
| :--- | :--- | :--- |
| `default_web_client_id not found` | Missing `oauth_client` (`client_type: 3`) in `google-services.json` | Enable Google provider in Firebase Auth, add SHA-1, then **re-download** `app/google-services.json`. |
| Sign-In sheet closes / `GetCredentialCancellationException` / Developer error (`10`) | APK signing SHA-1 does not match Firebase | Run `keytool -list -v` on the keystore signing your APK (`debug.keystore.base64` for GitHub builds) and add its SHA-1 to Firebase. |
| `Error 403: access_denied` for a player's Gmail | OAuth Consent Screen is in **Testing** mode | Add that Gmail under **OAuth consent screen → Test users**, or click **Publish App**. |
| `NoCredentialException` on silent login | First launch or no saved Google session yet | Tap the **Sign in with Google** button; on emulators ensure Google Play Services is installed and updated. |

### 6. Configure Cloud Firestore & Security Rules
1. In Firebase Console → **Build → Firestore Database**, ensure your database exists (named or `(default)`).
2. Update `firestore_database_id` in `app/src/main/res/values/firebase_applet_config.xml` to match your Firestore database ID (use `(default)` if using the default database).
3. Deploy the included security rules (`firestore.rules`) using Firebase CLI:
   ```bash
   firebase deploy --only firestore:rules
   ```

### 7. Configure Firebase App Check (Required for AI Parsers)
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
2. **If `app/google-services.json` is not committed to Git** (for example when pushing from AI Studio, or whenever you re-download it after adding an Android app/SHA-1), add or update the GitHub Repository Secret named `GOOGLE_SERVICES_JSON_BASE64` in **GitHub Repo → Settings → Secrets and variables → Actions → New repository secret**. You can paste either the **raw `google-services.json` text** directly or its **base64** output (`base64 -w 0 app/google-services.json`).
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
