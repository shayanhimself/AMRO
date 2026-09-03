package com.shayan.amro.core.network.sources.tmdb

import kotlinx.serialization.json.Json

/**
 * The parser the tests decode fixtures with, configured the way the client is.
 *
 * A test that decoded strictly would fail on the fields the production client is built to ignore.
 */
internal val testJson = Json { ignoreUnknownKeys = true }
