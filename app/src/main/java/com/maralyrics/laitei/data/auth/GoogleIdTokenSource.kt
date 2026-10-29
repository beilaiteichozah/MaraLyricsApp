package com.maralyrics.laitei.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.maralyrics.laitei.BuildConfig
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

    private companion object {
        const val TAG = "GoogleSignIn"
    }

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
            // Google also reports its own failures this way (an app it doesn't recognise comes back as
            // "[16] Account reauth failed."), so only a real "cancelled by the user" is left silent.
            if (BuildConfig.DEBUG) Log.w(TAG, "Google sign-in cancelled/failed: ${e.type} ${e.message}")
            if (GoogleSignInErrors.isUserCancel(e.message)) GoogleIdTokenResult.Cancelled
            else GoogleSignInErrors.classify(e.type, e.message)
        } catch (e: NoCredentialException) {
            GoogleIdTokenResult.NoAccount
        } catch (e: GetCredentialException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Google sign-in failed: ${e.type} ${e.message}")
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
    /** True for the reader backing out of the account chooser; false for a failure that merely arrives as a "cancel". */
    fun isUserCancel(message: String?): Boolean {
        val text = message?.lowercase() ?: return true // nothing said: the reader simply closed it
        return "by the user" in text || "user cancel" in text || "user_cancel" in text
    }

    fun classify(type: String?, message: String?): GoogleIdTokenResult {
        val text = "${type.orEmpty()} ${message.orEmpty()}".lowercase()
        return when {
            // "[16] Account reauth failed." is what an app Google doesn't recognise gets after the account is chosen
            // (the log says UNREGISTERED_ON_API_CONSOLE), as well as the plain 28444 developer error.
            "28444" in text || "developer console" in text || "not set up correctly" in text ||
                "reauth failed" in text || "unregistered" in text -> GoogleIdTokenResult.NotSetUp
            "no_credential" in text || "no credential" in text || "no accounts" in text -> GoogleIdTokenResult.NoAccount
            "cancel" in text -> GoogleIdTokenResult.Cancelled
            else -> GoogleIdTokenResult.Failed
        }
    }
}
