package com.maralyrics.laitei.data.repository

import com.maralyrics.laitei.data.local.SessionStore
import com.maralyrics.laitei.data.local.StoredSession
import com.maralyrics.laitei.data.remote.AccountApiService
import com.maralyrics.laitei.data.remote.dto.AccountConfigDto
import com.maralyrics.laitei.data.remote.dto.AccountDto
import com.maralyrics.laitei.data.remote.dto.GoogleAuthResponse
import com.maralyrics.laitei.data.remote.dto.GoogleSignInRequest
import com.maralyrics.laitei.data.remote.dto.AuthResponse
import com.maralyrics.laitei.data.remote.dto.LoginRequest
import com.maralyrics.laitei.data.remote.dto.MeResponse
import com.maralyrics.laitei.data.remote.dto.SyncRequest
import com.maralyrics.laitei.data.remote.dto.SyncResponse
import com.maralyrics.laitei.data.remote.dto.TokenResponse
import com.maralyrics.laitei.domain.model.Account
import com.maralyrics.laitei.domain.model.AuthException
import com.maralyrics.laitei.domain.model.AuthFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AccountRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val tokenA = "aaaa1111.bbbb2222.cccc3333"
    private val tokenB = "dddd4444.eeee5555.ffff6666"

    private val dto = AccountDto(
        id = 12, username = "mara_reader", displayName = "Mara Reader", contactEmail = "reader@example.com",
        emailVerified = JsonPrimitive(true), hasPassword = JsonPrimitive(true), google = JsonPrimitive(false)
    )

    private fun errorBody(text: String) = text.toResponseBody("application/json".toMediaType())

    /** An account server that answers however the test says, and remembers what it was asked. */
    private class FakeApi : AccountApiService {
        var login: suspend (LoginRequest) -> Response<AuthResponse> = { error("login not expected") }
        var me: suspend (String) -> Response<MeResponse> = { error("me not expected") }
        var logoutAll: suspend (String) -> Response<TokenResponse> = { error("logout-all not expected") }
        val loginRequests = mutableListOf<LoginRequest>()
        val meAuthorizations = mutableListOf<String>()

        override suspend fun login(body: LoginRequest): Response<AuthResponse> {
            loginRequests += body
            return login.invoke(body)
        }

        override suspend fun me(authorization: String): Response<MeResponse> {
            meAuthorizations += authorization
            return me.invoke(authorization)
        }

        var config: suspend () -> Response<AccountConfigDto> = { error("config not expected") }
        var google: suspend (GoogleSignInRequest) -> Response<GoogleAuthResponse> = { error("google not expected") }
        var configCalls = 0

        override suspend fun config(): Response<AccountConfigDto> { configCalls++; return config.invoke() }
        override suspend fun google(body: GoogleSignInRequest): Response<GoogleAuthResponse> = google.invoke(body)

        override suspend fun sync(authorization: String, body: SyncRequest): Response<SyncResponse> =
            error("sync not expected")

        override suspend fun logoutAll(authorization: String): Response<TokenResponse> = logoutAll.invoke(authorization)
    }

    private class MemoryStore(var saved: StoredSession? = null) : SessionStore {
        override suspend fun read() = saved
        override suspend fun write(session: StoredSession) { saved = session }
        override suspend fun clear() { saved = null }
    }

    private val api = FakeApi()
    private val store = MemoryStore()
    private fun repository() = AccountRepositoryImpl(api, store, json)

    // --- Signing in ---------------------------------------------------------------------------

    @Test
    fun `signing in keeps the session and shows the account`() = runBlocking {
        api.login = { Response.success(AuthResponse(tokenA, dto)) }
        val repo = repository()

        val account = repo.signIn("  Reader@Example.com ", "hunter22").getOrThrow()

        assertEquals("Mara Reader", account.label)
        assertEquals("reader@example.com", account.email)
        assertTrue(account.emailVerified)
        assertEquals(account, repo.account.value)
        assertEquals(tokenA, repo.tokenForWebsite())
        assertEquals("Bearer $tokenA", repo.authorization())
        assertEquals(12L, repo.accountId())
        assertEquals("the name is trimmed, the password is not touched", "Reader@Example.com", api.loginRequests.single().identifier)
        assertEquals("hunter22", api.loginRequests.single().password)
    }

    @Test
    fun `a saved session is still there after the app restarts`() = runBlocking {
        api.login = { Response.success(AuthResponse(tokenA, dto)) }
        repository().signIn("reader", "pw").getOrThrow()

        val afterRestart = repository()
        assertNull("nothing is loaded until asked", afterRestart.account.value)
        afterRestart.load()

        assertEquals("mara_reader", afterRestart.account.value?.username)
        assertEquals(tokenA, afterRestart.tokenForWebsite())
    }

    @Test
    fun `a wrong password signs nobody in and saves nothing`() = runBlocking {
        api.login = { Response.error(401, errorBody("""{"error":"Invalid email or password"}""")) }
        val repo = repository()

        val failure = (repo.signIn("reader", "nope").exceptionOrNull() as AuthException).failure

        assertEquals(AuthFailure.InvalidCredentials, failure)
        assertNull(repo.account.value)
        assertNull(store.saved)
        assertNull(repo.authorization())
    }

    @Test
    fun `the security check and the lockout are reported as such`() = runBlocking {
        val repo = repository()

        api.login = { Response.error(403, errorBody("""{"error":"Security check","code":"challenge_required"}""")) }
        assertEquals(AuthFailure.ChallengeRequired, (repo.signIn("a", "b").exceptionOrNull() as AuthException).failure)

        api.login = { Response.error(429, errorBody("""{"error":"Too many failed attempts. Try again in 15 minutes."}""")) }
        assertEquals(AuthFailure.TooManyAttempts(15), (repo.signIn("a", "b").exceptionOrNull() as AuthException).failure)
    }

    @Test
    fun `no connection is reported as offline, not as a wrong password`() = runBlocking {
        api.login = { throw IOException("no route to host") }

        val failure = (repository().signIn("a", "b").exceptionOrNull() as AuthException).failure

        assertEquals(AuthFailure.Offline, failure)
    }

    @Test
    fun `an answer without a token is not a sign-in`() = runBlocking {
        api.login = { Response.success(AuthResponse("", dto)) }
        val repo = repository()

        assertTrue(repo.signIn("a", "b").isFailure)
        assertNull(repo.account.value)
        assertNull(store.saved)
    }

    // --- A token from the website's own sign-in -----------------------------------------------

    @Test
    fun `text that is not a token is refused without asking the server`() = runBlocking {
        val repo = repository()

        assertTrue(repo.adoptToken("not-a-token").isFailure)
        assertTrue(repo.adoptToken("").isFailure)
        assertTrue(repo.adoptToken("a.b.c").isFailure)

        assertTrue(api.meAuthorizations.isEmpty())
        assertNull(repo.account.value)
    }

    @Test
    fun `a token from the website is checked with the server before it is trusted`() = runBlocking {
        api.me = { Response.success(MeResponse(dto)) }
        val repo = repository()

        val account = repo.adoptToken(tokenA).getOrThrow()

        assertEquals(listOf("Bearer $tokenA"), api.meAuthorizations)
        assertEquals("mara_reader", account.username)
        assertEquals(tokenA, store.saved?.token)
    }

    @Test
    fun `a token the server rejects is not kept`() = runBlocking {
        api.me = { Response.error(401, errorBody("""{"error":"Unauthorized"}""")) }
        val repo = repository()

        assertTrue(repo.adoptToken(tokenA).isFailure)
        assertNull(repo.account.value)
        assertNull(store.saved)
    }

    // --- Staying signed in --------------------------------------------------------------------

    private fun signedInRepository(): AccountRepositoryImpl {
        store.saved = StoredSession(tokenA, dto.let { Account(12, it.username, it.displayName, it.contactEmail, true) })
        return repository()
    }

    @Test
    fun `refreshing updates the account from the server`() = runBlocking {
        api.me = { Response.success(MeResponse(dto.copy(displayName = "New Name"))) }
        val repo = signedInRepository()

        repo.refresh()

        assertEquals("New Name", repo.account.value?.label)
        assertEquals("New Name", store.saved?.account?.label)
    }

    @Test
    fun `a session the server no longer accepts signs this device out`() = runBlocking {
        api.me = { Response.error(401, errorBody("""{"error":"Unauthorized"}""")) }
        val repo = signedInRepository()

        repo.refresh()

        assertNull(repo.account.value)
        assertNull(repo.authorization())
        assertNull("the saved copy goes too", store.saved)
    }

    @Test
    fun `being offline never signs anyone out`() = runBlocking {
        api.me = { throw IOException("offline") }
        val repo = signedInRepository()

        repo.refresh()

        assertNotNull(repo.account.value)
        assertEquals(tokenA, repo.tokenForWebsite())
        assertNotNull(store.saved)
    }

    @Test
    fun `a server hiccup never signs anyone out either`() = runBlocking {
        api.me = { Response.error(500, errorBody("""{"error":"oops"}""")) }
        val repo = signedInRepository()

        repo.refresh()

        assertNotNull(repo.account.value)
    }

    // --- Signing out --------------------------------------------------------------------------

    @Test
    fun `signing out forgets the session on this device`() = runBlocking {
        val repo = signedInRepository()
        repo.load()

        repo.signOut()

        assertNull(repo.account.value)
        assertNull(repo.tokenForWebsite())
        assertNull(repo.authorization())
        assertNull(store.saved)
    }

    @Test
    fun `signing out everywhere swaps in the fresh token so this device stays signed in`() = runBlocking {
        api.logoutAll = { Response.success(TokenResponse(tokenB)) }
        val repo = signedInRepository()

        repo.signOutEverywhere().getOrThrow()

        assertEquals("Bearer $tokenB", repo.authorization())
        assertEquals(tokenB, store.saved?.token)
        assertNotNull(repo.account.value)
    }

    @Test
    fun `if signing out everywhere fails the old session carries on`() = runBlocking {
        api.logoutAll = { throw IOException("offline") }
        val repo = signedInRepository()

        val failure = (repo.signOutEverywhere().exceptionOrNull() as AuthException).failure

        assertEquals(AuthFailure.Offline, failure)
        assertEquals("Bearer $tokenA", repo.authorization())
    }

    @Test
    fun `invalidating the session after the server turned it down signs out`() = runBlocking {
        val repo = signedInRepository()
        repo.load()

        repo.invalidate()

        assertFalse(repo.account.value != null)
        assertNull(store.saved)
    }
}
