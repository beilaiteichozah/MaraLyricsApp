package com.maralyrics.data.remote

import com.maralyrics.data.remote.dto.IncrementalSyncResponse
import com.maralyrics.data.remote.dto.SongsResponse
import com.maralyrics.data.remote.dto.VersionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MaraLyricsApiService {

    @GET("api/v1/songs")
    suspend fun getAllSongs(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): Response<SongsResponse>

    @GET("api/v1/version")
    suspend fun getDatabaseVersion(): Response<VersionResponse>

    @GET("api/v1/songs/sync")
    suspend fun getIncrementalUpdates(
        @Query("since_version") sinceVersion: Int
    ): Response<IncrementalSyncResponse>

    companion object {
        const val BASE_URL = "https://api.maralyrics.com/"
    }
}