package com.example.ui.components

import android.content.Context
import java.net.URL
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo

/**
 * Native voice room (Jitsi Meet SDK): voice, camera, text chat and PHONE SCREEN SHARE ("..." -> Share screen),
 * Discord-style. Free, no account. Returns false when the SDK could not start, so the caller can fall back to
 * the WebView room ([VoiceRoomDialog]).
 */
object VoiceRoomNative {
    const val SERVER = "https://meet.jit.si"

    fun launch(context: Context, room: String, displayName: String?, server: String = SERVER): Boolean = try {
        val user = JitsiMeetUserInfo()
        displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { user.displayName = it }
        val options = JitsiMeetConferenceOptions.Builder()
            .setServerURL(URL(server))
            .setRoom(room)
            .setUserInfo(user)
            .setAudioMuted(false)
            .setVideoMuted(true)
            .setConfigOverride("prejoinConfig.enabled", false)
            .setConfigOverride("disableDeepLinking", true)
            .setFeatureFlag("welcomepage.enabled", false)
            .setFeatureFlag("prejoinpage.enabled", false)
            .setFeatureFlag("android.screensharing.enabled", true)
            .setFeatureFlag("invite.enabled", false)
            .setFeatureFlag("calendar.enabled", false)
            .setFeatureFlag("call-integration.enabled", false)
            .setFeatureFlag("pip.enabled", true)
            .build()
        JitsiMeetActivity.launch(context, options)
        true
    } catch (t: Throwable) {
        android.util.Log.w("VoiceRoomNative", "native room failed, falling back to WebView", t)
        false
    }
}
