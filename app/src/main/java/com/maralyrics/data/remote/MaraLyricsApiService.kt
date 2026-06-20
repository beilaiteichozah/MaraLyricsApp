package com.maralyrics.data.remote

import com.maralyrics.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MaraLyricsApiService {

    @GET("api/v1/version")
    suspend fun getDatabaseVersion(): Response<VersionResponse>

    @GET("api/v1/bootstrap")
    suspend fun getBootstrap(): Response<BootstrapResponse>

    @GET("api/v1/songs")
    suspend fun getAllSongs(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): Response<SongsResponse>

    @GET("api/v1/songs/{slug}")
    suspend fun getSongBySlug(
        @Path("slug") slug: String
    ): Response<SongDto>

    @GET("api/v1/artists")
    suspend fun getAllArtists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): Response<ArtistsResponse>

    @GET("api/v1/artists/{slug}")
    suspend fun getArtistBySlug(
        @Path("slug") slug: String
    ): Response<ArtistDto>

    @GET("api/v1/composers")
    suspend fun getAllComposers(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): Response<ComposersResponse>

    @GET("api/v1/composers/{slug}")
    suspend fun getComposerBySlug(
        @Path("slug") slug: String
    ): Response<ComposerDto>

    @GET("api/v1/songs/sync")
    suspend fun getIncrementalUpdates(
        @Query("since_version") sinceVersion: Int
    ): Response<IncrementalSyncResponse>

    @POST("api/v1/feedback")
    @Deprecated("Use api/v1/report instead")
    suspend fun submitFeedback(
        @Body request: FeedbackRequest
    ): Response<Unit>

    @POST("api/v1/report")
    suspend fun submitReport(
        @Body request: ReportRequest
    ): Response<Unit>

    companion object {
        const val BASE_URL = "https://api.maralyrics.com/"
    }
}
