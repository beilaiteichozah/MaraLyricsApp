package com.maralyrics.laitei.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

// The reader-account API (api.maralyrics.com/api/v1/account/*), as the website's account page uses
// it. Every response field the app reads has a default, so a field the server adds or omits can't
// fail a sign-in.

@Serializable
data class LoginRequest(
    @SerialName("identifier") val identifier: String,
    @SerialName("password") val password: String,
    // Only sent after the server has asked for the security check, which the app can't show
    // natively; that case sends the reader to the website instead.
    @SerialName("turnstile_token") val turnstileToken: String? = null
)

@Serializable
data class AccountDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("username") val username: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
    @SerialName("email_verified") val emailVerified: JsonPrimitive? = null,
    @SerialName("has_password") val hasPassword: JsonPrimitive? = null,
    @SerialName("google") val google: JsonPrimitive? = null
)

@Serializable
data class AuthResponse(
    @SerialName("token") val token: String = "",
    @SerialName("account") val account: AccountDto = AccountDto()
)

@Serializable
data class MeResponse(
    @SerialName("account") val account: AccountDto = AccountDto()
)

/** logout-all, and the other calls that rotate the session, answer with a fresh token. */
@Serializable
data class TokenResponse(
    @SerialName("token") val token: String = ""
)

/** The shape of every error body: a message, and for a few cases a machine-readable code. */
@Serializable
data class ApiErrorDto(
    @SerialName("error") val error: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("code") val code: String? = null
)

// --- Sync -------------------------------------------------------------------------------------

@Serializable
data class SyncOpDto(
    @SerialName("kind") val kind: String,
    @SerialName("key") val key: String,
    @SerialName("at") val at: Long,
    @SerialName("deleted") val deleted: Boolean = false,
    @SerialName("value") val value: JsonPrimitive? = null
)

@Serializable
data class SyncRequest(
    @SerialName("since") val since: Long,
    @SerialName("ops") val ops: List<SyncOpDto>
)

@Serializable
data class SyncItemDto(
    @SerialName("kind") val kind: String = "",
    @SerialName("key") val key: String = "",
    @SerialName("value") val value: JsonPrimitive? = null,
    @SerialName("deleted") val deleted: JsonPrimitive? = null,
    @SerialName("at") val at: Long = 0
)

@Serializable
data class SyncResponse(
    @SerialName("rev") val rev: Long = 0,
    @SerialName("more") val more: JsonPrimitive? = null,
    @SerialName("items") val items: List<SyncItemDto> = emptyList(),
    @SerialName("truncated") val truncated: JsonPrimitive? = null
)
