package com.maralyrics.laitei.presentation.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Account
import com.maralyrics.laitei.domain.model.AccountSync
import com.maralyrics.laitei.domain.model.AuthException
import com.maralyrics.laitei.domain.model.AuthFailure
import com.maralyrics.laitei.domain.model.AccountSyncStatus
import com.maralyrics.laitei.domain.repository.AccountRepository
import com.maralyrics.laitei.data.auth.GoogleIdTokenResult
import com.maralyrics.laitei.data.auth.GoogleIdTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val isLoading: Boolean = false,
    val failure: AuthFailure? = null
)

/** One-off things the account screen tells the reader about. */
sealed interface AccountMessage {
    data object SignedOutEverywhere : AccountMessage
    data class Failed(val failure: AuthFailure) : AccountMessage
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val sync: AccountSync,
    private val googleSource: GoogleIdTokenSource
) : ViewModel() {

    val syncStatus: StateFlow<AccountSyncStatus> = sync.status

    fun syncNow() = sync.syncNow()

    val account: StateFlow<Account?> = repository.account

    private val _signIn = MutableStateFlow(SignInUiState())
    val signIn: StateFlow<SignInUiState> = _signIn.asStateFlow()

    private val messageChannel = Channel<AccountMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.load()
            repository.refresh()
        }
    }

    fun signIn(identifier: String, password: String) {
        if (_signIn.value.isLoading) return
        viewModelScope.launch {
            _signIn.value = SignInUiState(isLoading = true)
            repository.signIn(identifier, password)
                .onSuccess { _signIn.value = SignInUiState() }
                .onFailure { _signIn.value = SignInUiState(failure = it.toFailure()) }
        }
    }

    /**
     * Signs in with a Google account. [context] has to be the Activity: Android draws its account
     * picker over it. It is used for this one call and not kept.
     */
    fun signInWithGoogle(context: Context) {
        if (_signIn.value.isLoading) return
        viewModelScope.launch {
            _signIn.value = SignInUiState(isLoading = true)
            val clientId = repository.googleClientId()
            if (clientId == null) {
                _signIn.value = SignInUiState(failure = AuthFailure.Offline)
                return@launch
            }
            when (val result = googleSource.request(context, clientId)) {
                is GoogleIdTokenResult.Token -> repository.signInWithGoogle(result.idToken)
                    .onSuccess { _signIn.value = SignInUiState() }
                    .onFailure { _signIn.value = SignInUiState(failure = it.toFailure()) }
                // The reader closed the picker: not an error, so no message.
                GoogleIdTokenResult.Cancelled -> _signIn.value = SignInUiState()
                GoogleIdTokenResult.NoAccount -> _signIn.value = SignInUiState(failure = AuthFailure.GoogleNoAccount)
                GoogleIdTokenResult.NotSetUp -> _signIn.value = SignInUiState(failure = AuthFailure.GoogleNotSetUp)
                GoogleIdTokenResult.Failed -> _signIn.value = SignInUiState(failure = AuthFailure.GoogleFailed)
            }
        }
    }

    fun clearFailure() {
        if (_signIn.value.failure != null) _signIn.value = SignInUiState()
    }

    /** The website's sign-in page produced a token; make it this device's session. Returns whether it was accepted. */
    suspend fun adoptWebsiteToken(token: String): Boolean = repository.adoptToken(token).isSuccess

    /** The token the account page should open signed in with, or null when signed out. */
    fun tokenForWebsite(): String? = repository.tokenForWebsite()

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun signOutEverywhere() {
        viewModelScope.launch {
            repository.signOutEverywhere()
                .onSuccess { messageChannel.send(AccountMessage.SignedOutEverywhere) }
                .onFailure { messageChannel.send(AccountMessage.Failed(it.toFailure())) }
        }
    }

    private fun Throwable.toFailure(): AuthFailure =
        (this as? AuthException)?.failure ?: AuthFailure.Server(null)
}
