package com.shayan.amro.core.network

import com.shayan.amro.core.model.DataError
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs one API call to any source and turns anything it throws into a returned failure.
 *
 * @param block issues the request and reads the response.
 * @return what [block] produced, or the cause it could not run.
 */
internal suspend fun <T> apiCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> =
    try {
        block()
    } catch (cancellation: CancellationException) {
        // A [CancellationException] is rethrown, a cancelled read is not a failure to render.
        throw cancellation
    } catch (failure: Exception) {
        NetworkResult.Failure(failure.toDataError())
    }

/**
 * Maps API call exception to [DataError].
 */
private fun Throwable.toDataError(): DataError =
    when (this) {
        is UnresolvedAddressException -> DataError.NoConnectivity
        is IOException -> DataError.NoConnectivity
        else -> DataError.Server
    }
