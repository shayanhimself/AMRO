package com.shayan.amro.core.network.sources.tmdb.fixture

import kotlinx.serialization.json.Json

/**
 * The parser recorded bodies are read with, configured the way the client that recorded them is.
 *
 * A test that decoded strictly would fail on the fields the production client is built to ignore.
 */
val testJson = Json { ignoreUnknownKeys = true }
