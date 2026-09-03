package com.shayan.amro.core.network

import com.shayan.amro.core.model.DataError
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.cancellation.CancellationException

/**
 * The only place a thrown failure becomes a [DataError].
 *
 * Ktor's connect, socket and request timeouts are all [IOException]s, so they land on
 * [DataError.NoConnectivity] with the dropped connections. An unresolved host does not: the JDK
 * reports it as an [UnresolvedAddressException], which extends [IllegalArgumentException], so it is
 * named here to keep a device with no DNS reading as offline rather than as a server fault.
 *
 * Everything else, a malformed body included, is [DataError.Server]: the API answered and the
 * answer cannot become movies.
 */
private fun Throwable.toDataError(): DataError =
    when (this) {
        is UnresolvedAddressException -> DataError.NoConnectivity
        is IOException -> DataError.NoConnectivity
        else -> DataError.Server
    }

/**
 * Runs one call to any source and turns anything it throws into a returned failure.
 *
 * [block] returns the result itself rather than a bare value, because the two failures a response
 * decides, a non-2xx status and an empty results array, are read from a response that arrived
 * intact.
 *
 * A [CancellationException] is rethrown rather than mapped: a screen that navigated away cancels
 * its scope, and a cancelled read is not a failure to render.
 *
 * @param block issues the request and reads the response.
 * @return what [block] produced, or the cause it could not run.
 */
internal suspend fun <T> apiCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        NetworkResult.Failure(failure.toDataError())
    }
