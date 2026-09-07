package com.shayan.amro.core.network.sources.tmdb

import com.shayan.amro.core.network.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val BEARER_PREFIX = "Bearer "

private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val SOCKET_TIMEOUT_MILLIS = 10_000L
private const val REQUEST_TIMEOUT_MILLIS = 20_000L

/**
 * The HTTP client every TMDB endpoint is called through.
 *
 * @param config holds the base URL and access token for TMDB.
 * @param engine what actually moves the bytes. Taking it as a parameter is what lets a test pass
 * `MockEngine` and production pass OkHttp, and Ktor keeps both main-safe.
 */
internal fun tmdbHttpClient(
    config: TmdbConfig,
    engine: HttpClientEngine,
): HttpClient =
    HttpClient(engine) {
        // A non-2xx status is read from the response rather than thrown, because the status is
        // what decides the failure and no error body is ever parsed.
        expectSuccess = false

        install(ContentNegotiation) {
            // Everything TMDB sends beyond the declared wire shapes is dropped here rather than
            // failing the parse.
            json(Json { ignoreUnknownKeys = true })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }

        install(Logging) {
            this.logger = Logger.DEFAULT
            level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE
            // The credential is on every request, and logging is the one place that would
            // otherwise write it out.
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        defaultRequest {
            header(HttpHeaders.Authorization, BEARER_PREFIX + config.readAccessToken)
        }
    }
