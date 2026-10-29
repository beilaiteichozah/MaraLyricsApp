package com.maralyrics.laitei.data.local

import androidx.test.platform.app.InstrumentationRegistry
import com.maralyrics.laitei.domain.model.Account
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The session token never expires, so it is checked on a real device's Keystore: encrypted, private, removable. */
class KeystoreSessionStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = KeystoreSessionStore(context, Json { ignoreUnknownKeys = true })
    private val file = File(context.noBackupFilesDir, "account_session.bin")

    private val session = StoredSession(
        token = "aaaa1111.bbbb2222.cccc3333",
        account = Account(id = 12, username = "mara_reader", displayName = "Mara Reader", email = "reader@example.com", emailVerified = true)
    )

    @After
    fun tearDown() = runBlocking { store.clear() }

    @Test
    fun aSessionComesBackExactlyAsItWasSaved() = runBlocking {
        store.write(session)

        assertEquals(session, store.read())
    }

    @Test
    fun nothingSavedMeansSignedOut() = runBlocking {
        store.clear()

        assertNull(store.read())
    }

    @Test
    fun theTokenAndTheEmailAreNotReadableInTheFile() = runBlocking {
        store.write(session)

        val onDisk = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse("token in plain text", "aaaa1111" in onDisk)
        assertFalse("email in plain text", "reader@example.com" in onDisk)
    }

    @Test
    fun theFileIsKeptOutOfAndroidBackups() = runBlocking {
        store.write(session)

        // noBackupFilesDir is excluded from Auto Backup and device-to-device transfers.
        assertTrue(file.absolutePath.startsWith(context.noBackupFilesDir.absolutePath))
        assertTrue(file.isFile)
    }

    @Test
    fun clearingRemovesTheSession() = runBlocking {
        store.write(session)
        assertNotNull(store.read())

        store.clear()

        assertNull(store.read())
        assertFalse(file.exists())
    }

    @Test
    fun aDamagedFileIsTreatedAsSignedOutNotACrash() = runBlocking {
        store.write(session)
        val bytes = file.readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 0x5A).toByte()
        file.writeBytes(bytes)

        assertNull(store.read())
    }

    @Test
    fun saveTwiceKeepsTheLatest() = runBlocking {
        store.write(session)
        val newer = session.copy(token = "dddd4444.eeee5555.ffff6666")
        store.write(newer)

        assertEquals(newer, store.read())
    }
}
