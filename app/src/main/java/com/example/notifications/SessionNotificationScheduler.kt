package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.JsonCodecs
import com.example.data.ScheduledSessionRecord
import com.example.data.Story
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SessionNotificationScheduler {
    const val CHANNEL_SCHEDULE = "story_weaver_session_schedule"
    const val CHANNEL_REMINDER = "story_weaver_play_day_reminder"

    const val EXTRA_STORY_ID = "extra_story_id"
    const val EXTRA_STORY_TITLE = "extra_story_title"
    const val EXTRA_SESSION_MILLIS = "extra_session_millis"
    const val EXTRA_SESSION_NOTE = "extra_session_note"

    private const val PREFS_NAME = "story_weaver_reminders"
    private const val KEY_RECORDS_JSON = "scheduled_records_json"
    private const val KEY_NOTIFIED_PREFIX = "notified_schedule_"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val scheduleChannel = NotificationChannel(
                CHANNEL_SCHEDULE,
                "Session Schedules",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies party members of the scheduled day and hour to play."
            }
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Play-Day Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminder notification on the day of your scheduled game session."
            }
            manager.createNotificationChannel(scheduleChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun formatSessionDateTime(millis: Long): String {
        if (millis <= 0L) return "Not scheduled"
        val formatter = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    fun formatReminderDateTime(millis: Long): String {
        if (millis <= 0L) return ""
        val formatter = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    /**
     * Computes the play-day reminder timestamp:
     * - Default: 9:00 AM local time on the scheduled play day.
     * - If the session starts before 11:00 AM, 2 hours before the session start.
     * - If scheduling same-day after 9:00 AM, 1 hour before start (or 5 mins before / 10s from now if imminent).
     */
    fun computePlayDayReminderMillis(
        sessionAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long? {
        if (sessionAtMillis <= nowMillis) return null
        val cal = Calendar.getInstance().apply { timeInMillis = sessionAtMillis }
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val morningMillis = cal.timeInMillis

        val twoHoursBefore = sessionAtMillis - 2 * 60 * 60 * 1000L
        val candidate = if (twoHoursBefore < morningMillis) twoHoursBefore else morningMillis

        return when {
            candidate > nowMillis -> candidate
            sessionAtMillis - 60 * 60 * 1000L > nowMillis -> sessionAtMillis - 60 * 60 * 1000L
            sessionAtMillis - 5 * 60 * 1000L > nowMillis -> sessionAtMillis - 5 * 60 * 1000L
            else -> nowMillis + 10_000L
        }
    }

    fun syncCampaignNotification(
        context: Context,
        story: Story,
        forceImmediateAnnouncement: Boolean = false,
    ) {
        if (story.id.isBlank()) return
        val appContext = context.applicationContext
        ensureChannels(appContext)

        if (story.nextSessionAtMillis <= 0L) {
            cancelPlayDayReminder(appContext, story.id)
            return
        }

        val now = System.currentTimeMillis()
        if (story.nextSessionAtMillis <= now) {
            cancelPlayDayReminder(appContext, story.id)
            return
        }

        val record = ScheduledSessionRecord(
            storyId = story.id,
            storyTitle = story.title.ifBlank { "Campaign" },
            sessionAtMillis = story.nextSessionAtMillis,
            note = story.nextSessionNote,
        )
        upsertRecord(appContext, record)
        schedulePlayDayReminderAlarm(appContext, record)

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$KEY_NOTIFIED_PREFIX${story.id}"
        val lastNotified = prefs.getLong(key, 0L)
        if ((forceImmediateAnnouncement || lastNotified != story.nextSessionAtMillis) &&
            hasNotificationPermission(appContext)
        ) {
            prefs.edit().putLong(key, story.nextSessionAtMillis).apply()
            showScheduleAnnouncementNow(
                context = appContext,
                storyId = record.storyId,
                storyTitle = record.storyTitle,
                sessionAtMillis = record.sessionAtMillis,
                note = record.note,
            )
        }
    }

    fun syncAllCampaigns(context: Context, stories: List<Story>) {
        stories.forEach { story ->
            syncCampaignNotification(context, story, forceImmediateAnnouncement = false)
        }
    }

    @SuppressLint("MissingPermission")
    fun showScheduleAnnouncementNow(
        context: Context,
        storyId: String,
        storyTitle: String,
        sessionAtMillis: Long,
        note: String,
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val formatted = formatSessionDateTime(sessionAtMillis)
        val bodyText = buildString {
            append("Next session: $formatted")
            if (note.isNotBlank()) append("\n$note")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SCHEDULE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Session Scheduled: $storyTitle")
            .setContentText("Play Day: $formatted")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent(context, storyId.hashCode()))
            .build()

        NotificationManagerCompat.from(context)
            .notify(("sched_$storyId").hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    fun showPlayDayReminderNow(
        context: Context,
        storyId: String,
        storyTitle: String,
        sessionAtMillis: Long,
        note: String,
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sessionAtMillis))
        val bodyText = buildString {
            append("Today is play day for $storyTitle at $timeFmt!")
            if (note.isNotBlank()) append("\n$note")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Play Day Reminder: $storyTitle")
            .setContentText("Game starts today at $timeFmt")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent(context, ("rem_$storyId").hashCode()))
            .build()

        NotificationManagerCompat.from(context)
            .notify(("rem_$storyId").hashCode(), notification)
    }

    fun schedulePlayDayReminderAlarm(context: Context, record: ScheduledSessionRecord) {
        val triggerAt = computePlayDayReminderMillis(record.sessionAtMillis) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = buildReminderPendingIntent(context, record)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    fun cancelPlayDayReminder(context: Context, storyId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val dummyRecord = ScheduledSessionRecord(storyId, "", 0L, "")
        alarmManager?.cancel(buildReminderPendingIntent(context, dummyRecord))
        removeRecord(context, storyId)
    }

    fun rescheduleAllSavedReminders(context: Context) {
        val records = loadRecords(context)
        val now = System.currentTimeMillis()
        val active = records.filter { it.sessionAtMillis > now }
        saveRecords(context, active)
        active.forEach { schedulePlayDayReminderAlarm(context, it) }
    }

    private fun buildReminderPendingIntent(
        context: Context,
        record: ScheduledSessionRecord,
    ): PendingIntent {
        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = SessionReminderReceiver.ACTION_PLAY_DAY_REMINDER
            putExtra(EXTRA_STORY_ID, record.storyId)
            putExtra(EXTRA_STORY_TITLE, record.storyTitle)
            putExtra(EXTRA_SESSION_MILLIS, record.sessionAtMillis)
            putExtra(EXTRA_SESSION_NOTE, record.note)
        }
        return PendingIntent.getBroadcast(
            context,
            ("alarm_${record.storyId}").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildOpenAppIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun upsertRecord(context: Context, record: ScheduledSessionRecord) {
        val list = loadRecords(context).filterNot { it.storyId == record.storyId } + record
        saveRecords(context, list)
    }

    private fun removeRecord(context: Context, storyId: String) {
        val list = loadRecords(context).filterNot { it.storyId == storyId }
        saveRecords(context, list)
    }

    private fun loadRecords(context: Context): List<ScheduledSessionRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return JsonCodecs.decodeScheduledSessions(prefs.getString(KEY_RECORDS_JSON, "[]") ?: "[]")
    }

    private fun saveRecords(context: Context, records: List<ScheduledSessionRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECORDS_JSON, JsonCodecs.encodeScheduledSessions(records)).apply()
    }
}
