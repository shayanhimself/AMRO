package com.shayan.amro.core.network

import com.shayan.amro.core.model.DataError

/**
 * What one call to a [MovieRemoteDataSource] produced.
 */
sealed interface NetworkResult<out T> {
    data class Success<T>(
        val value: T,
    ) : NetworkResult<T>

    data class Failure(
        val error: DataError,
    ) : NetworkResult<Nothing>
}
