package com.maralyrics.laitei.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertTrue(GoogleSignInErrors.isUserCancel("activity is cancelled by the user."))
        assertTrue(GoogleSignInErrors.isUserCancel("Activity was canceled by the user"))
        assertTrue("no message at all: the reader just backed out", GoogleSignInErrors.isUserCancel(null))
    }

    @Test
    fun `an app Google does not recognise is not mistaken for the reader backing out`() {
        // Seen on a phone: the chooser closes after an account is picked, and Credential Manager reports it as a cancellation.
        val message = "[16] Account reauth failed."

        assertFalse(GoogleSignInErrors.isUserCancel(message))
        assertEquals(GoogleIdTokenResult.NotSetUp, GoogleSignInErrors.classify("androidx.credentials.TYPE_USER_CANCELED", message))
        assertEquals(GoogleIdTokenResult.NotSetUp, GoogleSignInErrors.classify(null, "status=UNREGISTERED_ON_API_CONSOLE"))
    }

    @Test
    fun `anything else is a plain failure`() {
        assertEquals(GoogleIdTokenResult.Failed, GoogleSignInErrors.classify("androidx.credentials.TYPE_UNKNOWN", "something broke"))
        assertEquals(GoogleIdTokenResult.Failed, GoogleSignInErrors.classify(null, null))
    }
}
