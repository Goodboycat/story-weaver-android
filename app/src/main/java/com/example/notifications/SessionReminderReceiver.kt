package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SessionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                SessionNotificationScheduler.rescheduleAllSavedReminders(context.applicationContext)
            }
            ACTION_PLAY_DAY_REMINDER -> {
                val storyId = intent.getStringExtra(SessionNotificationScheduler.EXTRA_STORY_ID) ?: return
                val title = intent.getStringExtra(SessionNotificationScheduler.EXTRA_STORY_TITLE) ?: "Campaign"
                val sessionMillis = intent.getLongExtra(SessionNotificationScheduler.EXTRA_SESSION_MILLIS, 0L)
                val note = intent.getStringExtra(SessionNotificationScheduler.EXTRA_SESSION_NOTE) ?: ""
                if (sessionMillis > 0L) {
                    SessionNotificationScheduler.showPlayDayReminderNow(
                        context = context.applicationContext,
                        storyId = storyId,
                        storyTitle = title,
                        sessionAtMillis = sessionMillis,
                        note = note,
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_DAY_REMINDER = "com.example.notifications.ACTION_PLAY_DAY_REMINDER"
    }
}
