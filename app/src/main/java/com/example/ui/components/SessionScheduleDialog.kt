package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Story
import com.example.notifications.SessionNotificationScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScheduleDialog(
    story: Story,
    isGm: Boolean,
    onSaveSchedule: (sessionAtMillis: Long, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(SessionNotificationScheduler.hasNotificationPermission(context))
    }
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDeniedNotice = !granted
        if (granted && story.nextSessionAtMillis > 0L) {
            SessionNotificationScheduler.syncCampaignNotification(
                context = context,
                story = story,
                forceImmediateAnnouncement = true,
            )
        }
    }

    val initialCal = remember(story.nextSessionAtMillis) {
        Calendar.getInstance().apply {
            if (story.nextSessionAtMillis > System.currentTimeMillis()) {
                timeInMillis = story.nextSessionAtMillis
            } else {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var sessionNote by remember { mutableStateOf(story.nextSessionNote) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val previewMillis = remember(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val previewDateText = remember(previewMillis) {
        SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(previewMillis))
    }
    val previewTimeText = remember(previewMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(previewMillis))
    }
    val previewReminderMillis = remember(previewMillis) {
        SessionNotificationScheduler.computePlayDayReminderMillis(previewMillis)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Play Schedule & Alerts", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Enable notifications to receive play-day alerts.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (permissionDeniedNotice) {
                                Text(
                                    "Notification permission was denied. Enable it in Android App Settings to get alerts.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("request_notification_permission_button")
                            ) {
                                Text("Allow Notifications")
                            }
                        }
                    }
                }

                // Active / Synced Schedule Summary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (story.nextSessionAtMillis > 0L) "Current Scheduled Game" else "No Session Scheduled Yet",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (story.nextSessionAtMillis > 0L) {
                                SessionNotificationScheduler.formatSessionDateTime(story.nextSessionAtMillis)
                            } else {
                                "Game Master can set the next play day & hour below."
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (story.nextSessionNote.isNotBlank()) {
                            Text(
                                text = story.nextSessionNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (story.nextSessionAtMillis > System.currentTimeMillis()) {
                            val activeRem = SessionNotificationScheduler.computePlayDayReminderMillis(story.nextSessionAtMillis)
                            if (activeRem != null) {
                                Text(
                                    text = "Play-day reminder: ${SessionNotificationScheduler.formatReminderDateTime(activeRem)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                if (isGm) {
                    HorizontalDivider()
                    Text(
                        "Set Next Play Day & Hour",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f).testTag("pick_session_date_button")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(previewDateText, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth().testTag("pick_session_time_button")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play Hour: $previewTimeText")
                    }

                    OutlinedTextField(
                        value = sessionNote,
                        onValueChange = { sessionNote = it.take(120) },
                        label = { Text("Session Note (optional)") },
                        placeholder = { Text("e.g., Session 5: Return to Phandalin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("session_note_input")
                    )

                    if (previewReminderMillis != null) {
                        Text(
                            text = "Play-day reminder will fire on ${SessionNotificationScheduler.formatReminderDateTime(previewReminderMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Choose a future day and hour to schedule party reminders.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Quick verification / preview button when notifications are allowed
                if (hasPermission && (story.nextSessionAtMillis > 0L || isGm)) {
                    OutlinedButton(
                        onClick = {
                            val targetMillis = if (story.nextSessionAtMillis > 0L) story.nextSessionAtMillis else previewMillis
                            val targetNote = if (story.nextSessionAtMillis > 0L) story.nextSessionNote else sessionNote
                            SessionNotificationScheduler.showScheduleAnnouncementNow(
                                context = context,
                                storyId = story.id,
                                storyTitle = story.title.ifBlank { "Campaign" },
                                sessionAtMillis = targetMillis,
                                note = targetNote,
                            )
                            SessionNotificationScheduler.showPlayDayReminderNow(
                                context = context,
                                storyId = story.id,
                                storyTitle = story.title.ifBlank { "Campaign" },
                                sessionAtMillis = targetMillis,
                                note = targetNote,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("test_session_notifications_button")
                    ) {
                        Text("Preview Both Notifications")
                    }
                }
            }
        },
        confirmButton = {
            if (isGm) {
                Button(
                    onClick = {
                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onSaveSchedule(previewMillis, sessionNote)
                    },
                    enabled = previewMillis > System.currentTimeMillis(),
                    modifier = Modifier.testTag("save_session_schedule_button")
                ) {
                    Text("Save & Notify")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isGm && story.nextSessionAtMillis > 0L) {
                    TextButton(
                        onClick = {
                            SessionNotificationScheduler.cancelPlayDayReminder(context, story.id)
                            onSaveSchedule(0L, "")
                        }
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
                if (isGm) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    )

    if (showDatePicker) {
        val utcInit = remember(selectedYear, selectedMonth, selectedDay) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(selectedYear, selectedMonth, selectedDay, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcInit)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            selectedYear = utcCal.get(Calendar.YEAR)
                            selectedMonth = utcCal.get(Calendar.MONTH)
                            selectedDay = utcCal.get(Calendar.DAY_OF_MONTH)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Play Hour") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
