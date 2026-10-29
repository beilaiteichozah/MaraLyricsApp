package com.maralyrics.laitei.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.maralyrics.laitei.domain.model.Account
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** What is kept between launches: the session token, and the account it belongs to (for showing offline). */
@Serializable
data class StoredSession(val token: String, val account: Account)

interface SessionStore {
    /** Null when signed out, or when the saved copy can't be read (a new device, a wiped key). */
    suspend fun read(): StoredSession?
    suspend fun write(session: StoredSession)
    suspend fun clear()
}

/**
 * Keeps the session in a file the Android Keystore encrypts. The token never expires on its own, so
 * it is treated like a password: encrypted at rest, kept out of Android's cloud backup (it lives in
 * `noBackupFilesDir`), and never logged.
 */
@Singleton
class KeystoreSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : SessionStore {

    private val file: File get() = File(context.noBackupFilesDir, FILE_NAME)

    override suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = file.takeIf { it.isFile }?.readBytes() ?: return@runCatching null
            json.decodeFromString<StoredSession>(String(SessionCrypto.decrypt(bytes), Charsets.UTF_8))
        }.getOrNull()
    }

    override suspend fun write(session: StoredSession) {
        withContext(Dispatchers.IO) {
            runCatching {
                val encrypted = SessionCrypto.encrypt(json.encodeToString(session).toByteArray(Charsets.UTF_8))
                val temp = File(file.parentFile, "$FILE_NAME.tmp")
                temp.writeBytes(encrypted)
                if (!temp.renameTo(file)) {
                    file.writeBytes(encrypted)
                    temp.delete()
                }
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { runCatching { file.delete() } }
    }

    private companion object {
        const val FILE_NAME = "account_session.bin"
    }
}

/** AES-256-GCM with a key that never leaves the Android Keystore. Output is `[ivLength][iv][ciphertext]`. */
internal object SessionCrypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "maralyrics_account_session"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray): ByteArray {
        val ivLength = data[0].toInt()
        val iv = data.copyOfRange(1, 1 + ivLength)
        val cipher = Cipher.getInstance(TRANSFORMATION)
            .apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv)) }
        return cipher.doFinal(data, 1 + ivLength, data.size - 1 - ivLength)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }
}
