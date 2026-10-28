package com.maralyrics.laitei.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.maralyrics.laitei.domain.model.EventReminders
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the "event today" reminder on and off. It is opt-in (Settings), so nothing is scheduled
 * until the reader asks for it. When on, a daily job wakes in the morning, looks up the day's
 * community events (from the saved copy when offline) and posts one notification.
 */
@Singleton
class EventReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    val isEnabled: Boolean get() = _enabled.value

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
        if (enabled) {
            // UPDATE so switching it on restarts the countdown to the next morning.
            schedule(ExistingPeriodicWorkPolicy.UPDATE)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    /** Called at app start: re-registers the job if it is on, without moving the time it is due. */
    fun ensureScheduled() {
        if (isEnabled) schedule(ExistingPeriodicWorkPolicy.KEEP)
    }

    private fun schedule(policy: ExistingPeriodicWorkPolicy) {
        val delay = EventReminders.delayUntilNextMorningMillis(System.currentTimeMillis(), TimeZone.getDefault())
        val request = PeriodicWorkRequestBuilder<EventReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val PREFS = "event_reminders"
        const val KEY_ENABLED = "enabled"
        const val WORK_NAME = "community_event_reminders"
    }
}
