package com.example

import com.example.notifications.SessionNotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SessionScheduleTest {

    @Test
    fun `evening future session triggers play day morning reminder at 9am`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val session = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 28, 19, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val reminder = SessionNotificationScheduler.computePlayDayReminderMillis(session, now)
        assertNotNull(reminder)

        val cal = Calendar.getInstance().apply { timeInMillis = reminder!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `early morning session triggers reminder 2 hours prior`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val session = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 28, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val reminder = SessionNotificationScheduler.computePlayDayReminderMillis(session, now)
        assertNotNull(reminder)
        assertEquals(session - 2 * 60 * 60 * 1000L, reminder)
    }

    @Test
    fun `past session returns null reminder`() {
        val now = 2_000_000_000_000L
        assertNull(SessionNotificationScheduler.computePlayDayReminderMillis(now - 1000L, now))
        val futureSameDay = now + 90 * 60 * 1000L
        val reminder = SessionNotificationScheduler.computePlayDayReminderMillis(futureSameDay, now)
        assertNotNull(reminder)
        assertTrue(reminder!! in (now + 1) until futureSameDay)
    }
}
