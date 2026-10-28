package com.maralyrics.laitei.presentation.common.notification

import android.app.NotificationManager
import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.test.platform.app.InstrumentationRegistry
import com.maralyrics.laitei.domain.model.CommunityEvent
import com.maralyrics.laitei.domain.model.EventMoment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Posts real notifications on the device: what the reader sees, and that it never repeats. */
class EventReminderNotifierTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val today = EventMoment.today()

    private fun event(id: String, title: String) = CommunityEvent(
        id = id, title = title, description = null, location = null, start = today, end = today
    )

    private fun posted(): List<StatusBarNotification> = manager.activeNotifications.filter { it.id == NOTIFICATION_ID }

    /** The system posts and cancels notifications asynchronously, so wait for the list to settle. */
    private fun awaitPosted(expected: Int, timeoutMs: Long = 5_000): List<StatusBarNotification> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var current = posted()
        while (current.size != expected && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(100)
            current = posted()
        }
        return current
    }

    private fun cancelAndWait() {
        manager.cancel(NOTIFICATION_ID)
        awaitPosted(0)
    }

    private fun clearLog() {
        context.getSharedPreferences("event_reminder_log", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Before
    fun setUp() {
        // Android 13+ won't show notifications until the permission is granted. The shell command
        // runs asynchronously, so read its output to the end to know it has finished.
        val output = instrumentation.uiAutomation
            .executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        clearLog()
        cancelAndWait()
    }

    @After
    fun tearDown() {
        cancelAndWait()
        clearLog()
    }

    @Test
    fun oneEventIsPostedWithItsTitle() {
        EventReminderNotifier(context).notifyToday(today, listOf(event("a", "Sunday School Exam")))

        val shown = awaitPosted(1).single().notification
        assertEquals("Event today", shown.extras.getCharSequence("android.title").toString())
        assertEquals("Sunday School Exam", shown.extras.getCharSequence("android.text").toString())
    }

    @Test
    fun severalEventsAreGatheredIntoOneNotification() {
        EventReminderNotifier(context).notifyToday(
            today,
            listOf(event("a", "Exam"), event("b", "Prayer meeting"), event("c", "Choir practice"))
        )

        val shown = awaitPosted(1).single().notification
        assertEquals("3 events today", shown.extras.getCharSequence("android.title").toString())
        val text = shown.extras.getCharSequence("android.text").toString()
        assertTrue(text, "Exam" in text && "Prayer meeting" in text && "Choir practice" in text)
    }

    @Test
    fun anEventAlreadyAnnouncedIsNotPostedAgainEvenAfterARestart() {
        EventReminderNotifier(context).notifyToday(today, listOf(event("a", "Exam")))
        assertEquals(1, awaitPosted(1).size)
        cancelAndWait()

        // A new instance, as after the app process was killed and the job ran again.
        EventReminderNotifier(context).notifyToday(today, listOf(event("a", "Exam")))
        Thread.sleep(1_500)

        assertTrue("the same event must not notify twice", posted().isEmpty())
    }

    @Test
    fun aNewEventAddedLaterTheSameDayIsStillAnnounced() {
        EventReminderNotifier(context).notifyToday(today, listOf(event("a", "Exam")))
        awaitPosted(1)
        cancelAndWait()

        EventReminderNotifier(context).notifyToday(today, listOf(event("a", "Exam"), event("b", "Choir")))

        val shown = awaitPosted(1).single().notification
        assertEquals("Event today", shown.extras.getCharSequence("android.title").toString())
        assertEquals("only the new one", "Choir", shown.extras.getCharSequence("android.text").toString())
    }

    @Test
    fun tappingItHasSomewhereToGo() {
        EventReminderNotifier(context).notifyToday(today, listOf(event("a", "Exam")))

        assertNotNull(awaitPosted(1).single().notification.contentIntent)
    }

    @Test
    fun noEventsTodayMeansNoNotification() {
        EventReminderNotifier(context).notifyToday(today, emptyList())
        Thread.sleep(1_000)

        assertTrue(posted().isEmpty())
    }

    private companion object {
        const val NOTIFICATION_ID = 1002
    }
}
