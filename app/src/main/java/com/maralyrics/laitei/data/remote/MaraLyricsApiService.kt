package com.maralyrics.laitei.data.remote

import com.maralyrics.laitei.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface MaraLyricsApiService {

    @GET("api/v1/version")
    suspend fun getDatabaseVersion(): Response<VersionResponse>

    @GET("api/v1/bootstrap")
    @Streaming
    suspend fun getBootstrap(
        @Query("since") since: String? = null
    ): Response<BootstrapResponse>

    @GET("api/v1/songs")
    suspend fun getAllSongs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("category") category: String? = null
    ): Response<SongsResponse>

    @GET("api/v1/songs/popular")
    suspend fun getPopularSongs(
        @Query("limit") limit: Int = 6
    ): Response<List<SongDto>>

    @GET("api/v1/songs/{slug}")
    suspend fun getSongBySlug(
        @Path("slug") slug: String
    ): Response<SongDto>

    @POST("api/v1/songs/{slug}/view")
    suspend fun incrementViewCount(
        @Path("slug") slug: String
    ): Response<Map<String, Boolean>>

    @GET("api/v1/search")
    suspend fun searchSongsRemote(
        @Query("q") query: String
    ): Response<SearchResponseDto>

    @GET("api/v1/categories")
    suspend fun getCategoriesRemote(): Response<CategoriesResponse>

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

    @GET("api/v1/stats")
    suspend fun getStatsRemote(): Response<StatsResponse>

    @GET("api/v1/contributors")
    suspend fun getContributorsRemote(): Response<ContributorsResponse>

    @POST("api/v1/reports")
    suspend fun submitReport(
        @Body request: ReportRequest
    ): Response<Unit>

    @POST("api/v1/contacts")
    suspend fun submitContact(
        @Body request: ContactRequest
    ): Response<Unit>

    companion object {
        const val BASE_URL = "https://api.maralyrics.com/"
    }
}
