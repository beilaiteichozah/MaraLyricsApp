package com.maralyrics.laitei.data.remote

import com.maralyrics.laitei.data.remote.dto.CalendarEventsResponse
import com.maralyrics.laitei.data.remote.dto.CalendarsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The Mara community calendar lives on its own service (calendar-api.marareih.org), separate from
 * the lyrics API — the maralyrics.com site reads it the same way.
 */
interface CalendarApiService {

    @GET("api/calendars")
    suspend fun getCalendars(): Response<CalendarsResponse>

    @GET("api/events")
    suspend fun getEvents(
        @Query("year") year: Int,
        @Query("calendar") calendarSlug: String
    ): Response<CalendarEventsResponse>

    companion object {
        const val BASE_URL = "https://calendar-api.marareih.org/"
    }
}
