# Voice Room (free, in-app)

## What is in the app now
Chat tab -> "Voice Room & Tools" -> **Join Audio** opens the NATIVE Jitsi Meet room (`VoiceRoomNative.kt`,
Jitsi Meet Android SDK 13.3.3, Apache-2.0). It is a screen inside Story Weaver - no other app, no account, no cost.
Every story has its own room (`aistudio_rpg_<storyId>`), so everyone who opens the same story lands in the same call.
Works: voice, camera, text chat, raise hand, tile view, picture-in-picture, and **phone screen share**:
in the call tap `...` (more) -> **Share screen** -> allow "Start recording or casting". Android shows a notification
while the screen is shared; tap `...` -> Stop sharing to end it.
If the SDK cannot start, the old WebView room (`VoiceRoomDialog.kt`, no phone screen share) opens instead.

Costs of the SDK: minSdk went 24 -> 26 (Android 8+), debug APK went 28 MB -> ~105 MB before the ABI filter
(`abiFilters` arm64-v8a + armeabi-v7a in `app/build.gradle.kts`; x86 emulators can no longer install it).
Not device-tested (CI-compiled only). To go back: `git revert` the "voice: native Jitsi Meet SDK" commits.

Known limit of the public server `meet.jit.si`: the first person in a room may be asked to log in
(Google/GitHub) as moderator, and calls on it are time-limited. Fine for a party of 3-6; see below if it annoys.

## If meet.jit.si gets annoying (owner steps, still free)
- Option A - Jitsi as a Service free tier (25 monthly active users): sign up at jaas.8x8.vc, copy the AppID,
  change the room URL in `VoiceRoomNative.SERVER` (and the WebView URL in `ChatTab.kt`) to `https://8x8.vc/<AppID>/<room>`. Needs a JWT for moderators.
- Option B - self-host Jitsi on a free VM (Oracle Cloud Always Free): install Docker, clone
  `jitsi/docker-jitsi-meet`, `cp env.example .env`, `./gen-passwords.sh`, `docker compose up -d`, point a free
  DuckDNS name at it, then replace `meet.jit.si` in `VoiceRoomNative.SERVER` (and the WebView URL in `ChatTab.kt`) with that host.
- Option C - LiveKit Cloud free tier (native SDK, best quality, Discord-like UI must be built by us; needs a
  tiny token server, e.g. a free Cloudflare Worker).
