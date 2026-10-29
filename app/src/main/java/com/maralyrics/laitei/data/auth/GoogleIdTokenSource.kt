package com.maralyrics.laitei.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

sealed interface GoogleIdTokenResult {
    data class Token(val idToken: String) : GoogleIdTokenResult

    /** The reader closed the account picker. Not a problem, so nothing is said. */
    data object Cancelled : GoogleIdTokenResult

    data object NoAccount : GoogleIdTokenResult

    /** Google doesn't recognise this build of the app as one allowed to sign people in. */
    data object NotSetUp : GoogleIdTokenResult

    data object Failed : GoogleIdTokenResult
}

/** Asks Android for the reader's Google account and returns Google's ID token for it. */
interface GoogleIdTokenSource {
    /**
     * [context] must be an Activity (the account picker is drawn over it); [serverClientId] is the
     * website's Google web client id, which the token has to be issued for so the server accepts it.
     */
    suspend fun request(context: Context, serverClientId: String): GoogleIdTokenResult
}

class CredentialManagerGoogleSource @Inject constructor() : GoogleIdTokenSource {

    override suspend fun request(context: Context, serverClientId: String): GoogleIdTokenResult {
        // "Sign in with Google" asked for by a tap on a button: shows Android's own account chooser.
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleIdTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleIdTokenResult.Failed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleIdTokenResult.NoAccount
        } catch (e: GetCredentialException) {
            GoogleSignInErrors.classify(e.type, e.message)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleIdTokenResult.Failed
        } catch (e: Exception) {
            GoogleIdTokenResult.Failed
        }
    }
}

internal object GoogleSignInErrors {

    /**
     * Google reports "this app isn't registered" as error 28444, "Developer console is not set up
     * correctly": the package name and signing key of the installed app have to be listed as an
     * Android OAuth client in the same Google Cloud project as the website's web client.
     */
    fun classify(type: String?, message: String?): GoogleIdTokenResult {
        val text = "${type.orEmpty()} ${message.orEmpty()}".lowercase()
        return when {
            "28444" in text || "developer console" in text || "not set up correctly" in text -> GoogleIdTokenResult.NotSetUp
            "no_credential" in text || "no credential" in text || "no accounts" in text -> GoogleIdTokenResult.NoAccount
            "cancel" in text -> GoogleIdTokenResult.Cancelled
            else -> GoogleIdTokenResult.Failed
        }
    }
}
