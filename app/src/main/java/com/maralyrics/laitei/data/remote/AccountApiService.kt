package com.maralyrics.laitei.data.remote

import com.maralyrics.laitei.data.remote.dto.AccountConfigDto
import com.maralyrics.laitei.data.remote.dto.AuthResponse
import com.maralyrics.laitei.data.remote.dto.GoogleAuthResponse
import com.maralyrics.laitei.data.remote.dto.GoogleSignInRequest
import com.maralyrics.laitei.data.remote.dto.LoginRequest
import com.maralyrics.laitei.data.remote.dto.MeResponse
import com.maralyrics.laitei.data.remote.dto.SyncRequest
import com.maralyrics.laitei.data.remote.dto.SyncResponse
import com.maralyrics.laitei.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Reader accounts. Calls that need a signed-in reader take the full `Authorization` value
 * ("Bearer <token>") explicitly instead of through an interceptor, so a token can never be
 * attached to a request that isn't meant to carry one.
 */
interface AccountApiService {

    @GET("api/v1/account/config")
    suspend fun config(): Response<AccountConfigDto>

    /** Signs in with a Google ID token. For a new account the server also wants its security check. */
    @POST("api/v1/account/google")
    suspend fun google(@Body body: GoogleSignInRequest): Response<GoogleAuthResponse>

    @POST("api/v1/account/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("api/v1/account/me")
    suspend fun me(@Header("Authorization") authorization: String): Response<MeResponse>

    @POST("api/v1/account/sync")
    suspend fun sync(
        @Header("Authorization") authorization: String,
        @Body body: SyncRequest
    ): Response<SyncResponse>

    /** Ends every other session of the account; the answer carries a token for this device. */
    @POST("api/v1/account/logout-all")
    suspend fun logoutAll(@Header("Authorization") authorization: String): Response<TokenResponse>
}
