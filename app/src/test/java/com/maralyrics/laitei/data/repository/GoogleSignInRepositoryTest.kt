package com.maralyrics.laitei.data.repository

import com.maralyrics.laitei.data.local.SessionStore
import com.maralyrics.laitei.data.local.StoredSession
import com.maralyrics.laitei.data.remote.AccountApiService
import com.maralyrics.laitei.data.remote.dto.AccountConfigDto
import com.maralyrics.laitei.data.remote.dto.AccountDto
import com.maralyrics.laitei.data.remote.dto.AuthResponse
import com.maralyrics.laitei.data.remote.dto.GoogleAuthResponse
import com.maralyrics.laitei.data.remote.dto.GoogleSignInRequest
import com.maralyrics.laitei.data.remote.dto.LoginRequest
import com.maralyrics.laitei.data.remote.dto.MeResponse
import com.maralyrics.laitei.data.remote.dto.SyncRequest
import com.maralyrics.laitei.data.remote.dto.SyncResponse
import com.maralyrics.laitei.data.remote.dto.TokenResponse
import com.maralyrics.laitei.domain.model.AuthException
import com.maralyrics.laitei.domain.model.AuthFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Signing in with the Google ID token the phone's account picker hands back. */
class GoogleSignInRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val token = "aaaa1111.bbbb2222.cccc3333"
    private val dto = AccountDto(id = 21, username = "g_reader", contactEmail = "reader@gmail.com", google = JsonPrimitive(true))

    private fun errorBody(text: String) = text.toResponseBody("application/json".toMediaType())

    private class Api : AccountApiService {
        var config: () -> Response<AccountConfigDto> = { error("config not expected") }
        var google: (GoogleSignInRequest) -> Response<GoogleAuthResponse> = { error("google not expected") }
        var configCalls = 0
        val googleRequests = mutableListOf<GoogleSignInRequest>()

        override suspend fun config(): Response<AccountConfigDto> { configCalls++; return config.invoke() }
        override suspend fun google(body: GoogleSignInRequest): Response<GoogleAuthResponse> {
            googleRequests += body
            return google.invoke(body)
        }
        override suspend fun login(body: LoginRequest): Response<AuthResponse> = error("not expected")
        override suspend fun me(authorization: String): Response<MeResponse> = error("not expected")
        override suspend fun sync(authorization: String, body: SyncRequest): Response<SyncResponse> = error("not expected")
        override suspend fun logoutAll(authorization: String): Response<TokenResponse> = error("not expected")
    }

    private class Store(var saved: StoredSession? = null) : SessionStore {
        override suspend fun read() = saved
        override suspend fun write(session: StoredSession) { saved = session }
        override suspend fun clear() { saved = null }
    }

    private val api = Api()
    private val store = Store()
    private val repo = AccountRepositoryImpl(api, store, json)

    private fun failure(result: Result<*>) = (result.exceptionOrNull() as AuthException).failure

    // --- The client id ------------------------------------------------------------------------

    @Test
    fun `the Google client id comes from the website's config`() = runBlocking {
        api.config = { Response.success(AccountConfigDto("1008-abc.apps.googleusercontent.com", JsonPrimitive(true))) }

        assertEquals("1008-abc.apps.googleusercontent.com", repo.googleClientId())
    }

    @Test
    fun `the client id is asked for once and then remembered`() = runBlocking {
        api.config = { Response.success(AccountConfigDto("client-id", null)) }

        repo.googleClientId()
        repo.googleClientId()
        repo.googleClientId()

        assertEquals(1, api.configCalls)
    }

    @Test
    fun `without a client id there is no Google sign-in, and a retry asks again`() = runBlocking {
        api.config = { Response.success(AccountConfigDto("  ", null)) }
        assertNull(repo.googleClientId())

        api.config = { throw IOException("offline") }
        assertNull(repo.googleClientId())

        api.config = { Response.error(500, errorBody("{}")) }
        assertNull(repo.googleClientId())

        api.config = { Response.success(AccountConfigDto("client-id", null)) }
        assertEquals("client-id", repo.googleClientId())
    }

    // --- Signing in ---------------------------------------------------------------------------

    @Test
    fun `a Google token the server accepts signs the reader in and is remembered`() = runBlocking {
        api.google = { Response.success(GoogleAuthResponse(token, dto, JsonPrimitive(false))) }

        val account = repo.signInWithGoogle("google-id-token").getOrThrow()

        assertEquals("g_reader", account.username)
        assertTrue(account.hasGoogle)
        assertEquals("google-id-token", api.googleRequests.single().credential)
        assertNull("no security-check token is sent", api.googleRequests.single().turnstileToken)
        assertEquals(account, repo.account.value)
        assertEquals(token, store.saved?.token)
        assertEquals("Bearer $token", repo.authorization())
    }

    @Test
    fun `a Google account with no MaraLyrics account yet is sent to the website`() = runBlocking {
        api.google = {
            Response.error(403, errorBody("""{"error":"Please complete the security check to create your account.","code":"challenge_required"}"""))
        }

        assertEquals(AuthFailure.NewGoogleAccount, failure(repo.signInWithGoogle("t")))
        assertNull(repo.account.value)
        assertNull(store.saved)
    }

    @Test
    fun `an email already tied to another Google account keeps the server's own explanation`() = runBlocking {
        api.google = {
            Response.error(409, errorBody("""{"error":"This email is already linked to a different Google account."}"""))
        }

        assertEquals(
            AuthFailure.Server("This email is already linked to a different Google account."),
            failure(repo.signInWithGoogle("t"))
        )
    }

    @Test
    fun `a Google token the server rejects is a Google problem, not a wrong password`() = runBlocking {
        api.google = { Response.error(401, errorBody("""{"error":"Unauthorized"}""")) }

        assertEquals(AuthFailure.GoogleFailed, failure(repo.signInWithGoogle("t")))
    }

    @Test
    fun `being offline while signing in with Google is reported as offline`() = runBlocking {
        api.google = { throw IOException("no route") }

        assertEquals(AuthFailure.Offline, failure(repo.signInWithGoogle("t")))
    }

    @Test
    fun `an answer without a token is not a sign-in`() = runBlocking {
        api.google = { Response.success(GoogleAuthResponse("", dto)) }

        assertTrue(repo.signInWithGoogle("t").isFailure)
        assertNull(repo.account.value)
    }
}
