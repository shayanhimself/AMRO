package com.shayan.amro.core.data

import com.shayan.amro.core.model.DataError

/**
 * A [DataError] on its way through a paging load state, which reports a failure as a `Throwable`.
 *
 * @property error why the fetch failed.
 */
class DataErrorException(
    val error: DataError,
) : Exception()
