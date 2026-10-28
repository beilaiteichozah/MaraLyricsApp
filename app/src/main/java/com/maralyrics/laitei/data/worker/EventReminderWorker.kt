package com.maralyrics.laitei.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maralyrics.laitei.domain.model.CommunityEvent
import com.maralyrics.laitei.domain.model.EventMoment
import com.maralyrics.laitei.domain.repository.CalendarRepository
import com.maralyrics.laitei.presentation.common.notification.EventReminderNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** Posts today's community events as a notification, once per event. Runs once a day when reminders are on. */
@HiltWorker
class EventReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: EventReminderScheduler,
    private val calendarRepository: CalendarRepository,
    private val notifier: EventReminderNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!scheduler.isEnabled) return Result.success()
        return try {
            val today = EventMoment.today()
            notifier.notifyToday(today, eventsInYear(today.year))
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Not worth retrying and risking a late or doubled notification; tomorrow's run tries again.
            Result.success()
        }
    }

    /** Every calendar's events for [year]; uses the saved copy of whatever can't be fetched. */
    private suspend fun eventsInYear(year: Int): List<CommunityEvent> =
        calendarRepository.getCalendars().getOrNull()?.data.orEmpty().flatMap { calendar ->
            calendarRepository.getEvents(calendar.slug, year).getOrNull()?.data.orEmpty()
        }
}
