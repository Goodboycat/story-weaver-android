# Voice Room (free, in-app)

## What is in the app now
Chat tab -> "Voice Room & Tools" -> **Join Audio** opens `VoiceRoomDialog`: the Jitsi Meet web client inside
a full-screen WebView. Nobody leaves Story Weaver, nobody needs an account, it costs nothing. Every story
has its own room (`aistudio_rpg_<storyId>`). Mic + camera permission are asked the first time.
Works: voice, camera, Jitsi text chat, seeing a screen that a desktop-browser player shares.
Does NOT work: sharing the *phone* screen (Android WebView has no `getDisplayMedia`).

Known limit of the public server `meet.jit.si`: the first person in a room may be asked to log in
(Google/GitHub) as moderator, and rooms are capped in size/time. Fine for a party of 3-6.

## Phone screen share = step 2 (free, not done yet)
Use the native Jitsi Meet Android SDK (Apache-2.0, free). It supports Android screen share out of the box.
1. `settings.gradle.kts` -> `dependencyResolutionManagement.repositories`: add
   `maven { url = uri("https://github.com/jitsi/jitsi-maven-repository/raw/master/releases") }`
2. `app/build.gradle.kts`: `implementation("org.jitsi.react:jitsi-meet-sdk:<latest>") { isTransitive = true }`
   (check the current version on the Jitsi handbook page "Android SDK"); minSdk must be >= 24 (already is).
3. Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION` permissions.
4. Replace the body of `VoiceRoomDialog` call site with
   `JitsiMeetActivity.launch(context, JitsiMeetConferenceOptions.Builder().setServerURL(URL("https://meet.jit.si"))
   .setRoom(roomSlug).setAudioOnly(true).setFeatureFlag("android.screensharing.enabled", true).build())`.
Cost: APK grows by ~60-80 MB (React Native inside). That is why it is a separate step.

## If meet.jit.si gets annoying (owner steps, still free)
- Option A - Jitsi as a Service free tier (25 monthly active users): sign up at jaas.8x8.vc, copy the AppID,
  change the room URL in `ChatTab.kt` to `https://8x8.vc/<AppID>/<room>`. Needs a JWT for moderators.
- Option B - self-host Jitsi on a free VM (Oracle Cloud Always Free): install Docker, clone
  `jitsi/docker-jitsi-meet`, `cp env.example .env`, `./gen-passwords.sh`, `docker compose up -d`, point a free
  DuckDNS name at it, then replace `meet.jit.si` in `ChatTab.kt` with that host.
- Option C - LiveKit Cloud free tier (native SDK, best quality, Discord-like UI must be built by us; needs a
  tiny token server, e.g. a free Cloudflare Worker).
