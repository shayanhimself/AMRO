package com.shayan.amro.core.model

/**
 * Why an API call failed.
 */
sealed interface DataError {
    /** The request never reached the API. */
    data object NoConnectivity : DataError

    /** The API answered, and the answer was not one that can become movies. */
    data object Server : DataError

    /** The API answered successfully with nothing in it. */
    data object EmptyResponse : DataError
}
