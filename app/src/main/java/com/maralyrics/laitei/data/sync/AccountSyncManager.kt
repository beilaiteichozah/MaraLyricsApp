package com.maralyrics.laitei.data.sync

import com.maralyrics.laitei.domain.model.AccountSync
import com.maralyrics.laitei.domain.model.SyncRecorder
import com.maralyrics.laitei.domain.model.AccountSyncStatus
import com.maralyrics.laitei.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides when the sync runs: when someone signs in, a moment after something changes, when the app
 * comes to the front (not more than once a minute), and when asked. The work itself is the
 * [AccountSyncEngine]'s.
 */
@Singleton
class AccountSyncManager @Inject constructor(
    private val engine: AccountSyncEngine,
    private val accounts: AccountRepository
) : SyncRecorder, AccountSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _status = MutableStateFlow(AccountSyncStatus())
    override val status: StateFlow<AccountSyncStatus> = _status.asStateFlow()

    private var pendingRun: Job? = null

    @Volatile
    private var lastRunAt = 0L

    /** Starts following who is signed in. Call once, when the app starts. */
    fun start() {
        scope.launch {
            accounts.load()
            accounts.account.map { it?.id }.distinctUntilChanged().collect { id ->
                if (id == null) {
                    engine.reset()
                    _status.value = AccountSyncStatus()
                } else {
                    run()
                }
            }
        }
    }

    override fun syncNow() {
        scope.launch { run() }
    }

    override fun onAppResumed() {
        if (accounts.account.value != null && System.currentTimeMillis() - lastRunAt > MIN_GAP_MS) syncNow()
    }

    override fun favoriteChanged(slug: String, isFavorite: Boolean) {
        scope.launch {
            engine.recordFavorite(slug, isFavorite)
            syncSoon()
        }
    }

    override fun favoritesAdded(slugs: Collection<String>) {
        scope.launch {
            slugs.forEach { engine.recordFavorite(it, isFavorite = true) }
            syncSoon()
        }
    }

    override fun preferenceChanged(key: String, value: String) {
        scope.launch {
            engine.recordPreference(key, value)
            syncSoon()
        }
    }

    /** Waits a moment, so a run of taps becomes one request. */
    private fun syncSoon() {
        if (accounts.account.value == null) return
        pendingRun?.cancel()
        pendingRun = scope.launch {
            delay(DEBOUNCE_MS)
            run()
        }
    }

    private suspend fun run() {
        _status.value = _status.value.copy(isSyncing = true)
        val outcome = engine.sync()
        lastRunAt = System.currentTimeMillis()
        _status.value = when (outcome) {
            is SyncOutcome.Success -> AccountSyncStatus(lastSyncedAt = lastRunAt)
            SyncOutcome.SignedOut -> AccountSyncStatus()
            SyncOutcome.Offline, SyncOutcome.Failed ->
                _status.value.copy(isSyncing = false, lastAttemptFailed = true)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 2_000L
        const val MIN_GAP_MS = 60_000L
    }
}
