package com.shayan.amro.core.network

/**
 * What the TMDB client needs to reach the API.
 *
 * @property baseUrl the root every endpoint is resolved against. It ends in a slash.
 * @property readAccessToken the v4 credential sent as a bearer token.
 */
data class TmdbConfig(
    val baseUrl: String,
    val readAccessToken: String,
)
