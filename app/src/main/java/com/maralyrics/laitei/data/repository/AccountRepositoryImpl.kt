package com.maralyrics.laitei.data.repository

import com.maralyrics.laitei.data.local.SessionStore
import com.maralyrics.laitei.data.local.StoredSession
import com.maralyrics.laitei.data.mapper.toDomain
import com.maralyrics.laitei.data.remote.AccountApiService
import com.maralyrics.laitei.data.remote.dto.LoginRequest
import com.maralyrics.laitei.domain.model.Account
import com.maralyrics.laitei.domain.model.AuthException
import com.maralyrics.laitei.domain.model.AuthFailure
import com.maralyrics.laitei.domain.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** What the sync engine needs from a signed-in session, without seeing how it is kept. */
interface SessionTokens {
    /** `Bearer <token>`, or null when signed out. */
    fun authorization(): String?
    fun accountId(): Long?

    /** The server stopped accepting the token (changed password, signed out everywhere): sign out here too. */
    suspend fun invalidate()
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: AccountApiService,
    private val store: SessionStore,
    private val json: Json
) : AccountRepository, SessionTokens {

    private val _account = MutableStateFlow<Account?>(null)
    override val account: StateFlow<Account?> = _account.asStateFlow()

    @Volatile
    private var token: String? = null

    private val loadLock = Mutex()
    private var loaded = false

    override suspend fun load() {
        loadLock.withLock {
            if (loaded) return
            store.read()?.let { saved ->
                token = saved.token
                _account.value = saved.account
            }
            loaded = true
        }
    }

    override suspend fun signIn(identifier: String, password: String): Result<Account> {
        load()
        val response = try {
            api.login(LoginRequest(identifier = identifier.trim(), password = password))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return failure(AuthFailure.Offline)
        } catch (e: Exception) {
            return failure(AuthFailure.Server(null))
        }

        val body = response.body()
        return if (response.isSuccessful && body != null && body.token.isNotBlank()) {
            val account = body.account.toDomain()
            adopt(body.token, account)
            Result.success(account)
        } else if (response.isSuccessful) {
            failure(AuthFailure.Server(null))
        } else {
            failure(AccountErrors.classify(response.code(), response.errorBodyText(), json))
        }
    }

    override suspend fun adoptToken(token: String): Result<Account> {
        load()
        if (!TOKEN_SHAPE.matches(token)) return failure(AuthFailure.InvalidCredentials)
        val response = try {
            api.me("Bearer $token")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return failure(AuthFailure.Offline)
        } catch (e: Exception) {
            return failure(AuthFailure.Server(null))
        }

        val body = response.body()
        return if (response.isSuccessful && body != null) {
            val account = body.account.toDomain()
            adopt(token, account)
            Result.success(account)
        } else {
            failure(AccountErrors.classify(response.code(), response.errorBodyText(), json))
        }
    }

    override suspend fun refresh() {
        load()
        val current = token ?: return
        val response = try {
            api.me("Bearer $current")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return // Offline or a server hiccup: stay signed in and try again later.
        }

        when {
            response.isSuccessful -> response.body()?.let { adopt(current, it.account.toDomain()) }
            response.code() == 401 -> signOut()
        }
    }

    override suspend fun signOutEverywhere(): Result<Unit> {
        load()
        val current = token ?: return failure(AuthFailure.InvalidCredentials)
        val response = try {
            api.logoutAll("Bearer $current")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return failure(AuthFailure.Offline)
        } catch (e: Exception) {
            return failure(AuthFailure.Server(null))
        }

        val fresh = response.body()?.token?.takeIf { response.isSuccessful && it.isNotBlank() }
        val account = _account.value
        return if (fresh != null && account != null) {
            adopt(fresh, account)
            Result.success(Unit)
        } else {
            failure(AccountErrors.classify(response.code(), response.errorBodyText(), json))
        }
    }

    override fun tokenForWebsite(): String? = token

    override suspend fun signOut() {
        load()
        token = null
        _account.value = null
        store.clear()
    }

    // --- SessionTokens (used by the sync engine) ---

    override fun authorization(): String? = token?.let { "Bearer $it" }

    override fun accountId(): Long? = _account.value?.id

    override suspend fun invalidate() = signOut()

    private suspend fun adopt(newToken: String, account: Account) {
        token = newToken
        _account.value = account
        store.write(StoredSession(newToken, account))
    }

    private fun <T> failure(reason: AuthFailure): Result<T> = Result.failure(AuthException(reason))

    private fun Response<*>.errorBodyText(): String? =
        try { errorBody()?.string() } catch (e: Exception) { null }

    private companion object {
        // A JWT: three base64url parts. Anything else is not a token, and must never be passed on.
        val TOKEN_SHAPE = Regex("""[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,}""")
    }
}
