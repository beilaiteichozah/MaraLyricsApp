package com.maralyrics.laitei.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleSignInErrorsTest {

    @Test
    fun `an app Google does not recognise is reported as not set up`() {
        // What Credential Manager raises when the package and signing key aren't registered as an Android OAuth client.
        assertEquals(
            GoogleIdTokenResult.NotSetUp,
            GoogleSignInErrors.classify("android.credentials.GetCredentialException.TYPE_UNKNOWN", "[28444] Developer console is not set up correctly.")
        )
        assertEquals(GoogleIdTokenResult.NotSetUp, GoogleSignInErrors.classify(null, "Error 28444"))
        assertEquals(GoogleIdTokenResult.NotSetUp, GoogleSignInErrors.classify("x", "the developer console is not set up correctly"))
    }

    @Test
    fun `no Google account on the phone is its own case`() {
        assertEquals(GoogleIdTokenResult.NoAccount, GoogleSignInErrors.classify("androidx.credentials.TYPE_NO_CREDENTIAL", "No credentials available"))
        assertEquals(GoogleIdTokenResult.NoAccount, GoogleSignInErrors.classify("x", "No accounts found"))
    }

    @Test
    fun `closing the account picker is not an error`() {
        assertEquals(GoogleIdTokenResult.Cancelled, GoogleSignInErrors.classify("androidx.credentials.TYPE_USER_CANCELED", "activity is cancelled by the user."))
    }

    @Test
    fun `anything else is a plain failure`() {
        assertEquals(GoogleIdTokenResult.Failed, GoogleSignInErrors.classify("androidx.credentials.TYPE_UNKNOWN", "something broke"))
        assertEquals(GoogleIdTokenResult.Failed, GoogleSignInErrors.classify(null, null))
    }
}
