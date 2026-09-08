package com.shayan.amro.core.network.sources.tmdb

/**
 * What the TMDB client needs to reach the API.
 *
 * @property readAccessToken the v4 credential sent as a bearer token.
 */
internal data class TmdbConfig(
    val readAccessToken: String,
)
