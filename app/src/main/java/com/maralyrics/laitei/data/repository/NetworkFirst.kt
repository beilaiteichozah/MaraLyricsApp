package com.maralyrics.laitei.data.repository

import com.maralyrics.laitei.data.local.ContentCache
import com.maralyrics.laitei.domain.model.ContentResult
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

/** True when a saved copy [ageMillis] old is recent enough to use without asking the network. */
internal fun isFresh(ageMillis: Long?, maxAgeMillis: Long): Boolean =
    ageMillis != null && ageMillis in 0 until maxAgeMillis

/**
 * Fetches from the network and saves the body under [key]; if that fails for any reason
 * (offline, HTTP error, malformed body) falls back to the last saved copy. Fails only when
 * there is neither, so callers can tell "nothing to show" apart from "showing saved data".
 */
internal suspend inline fun <reified T : Any> ContentCache.networkFirst(
    key: String,
    crossinline fetch: suspend () -> Response<T>
): Result<ContentResult<T>> {
    val fresh: T? = try {
        fetch().takeIf { it.isSuccessful }?.body()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    if (fresh != null) {
        writeJson(key, fresh)
        return Result.success(ContentResult(fresh, isFromCache = false))
    }

    val saved = readJson<T>(key)
    return if (saved != null) {
        Result.success(ContentResult(saved, isFromCache = true))
    } else {
        Result.failure(IOException("Could not load $key and no saved copy exists"))
    }
}
