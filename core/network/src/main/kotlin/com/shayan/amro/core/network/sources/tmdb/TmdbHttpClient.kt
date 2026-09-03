package com.shayan.amro.core.network.sources.tmdb

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
 * The client every TMDB endpoint is called through.
 *
 * @param config the address to resolve endpoints against and the credential to send.
 * @param engine what actually moves the bytes. Taking it as a parameter is what lets a test pass
 * `MockEngine` and production pass OkHttp, and Ktor keeps both main-safe.
 * @param logger where the client writes. A test reads what was written to prove the credential is
 * not in it.
 * @param logLevel how much is written. A test raises it to the level that logs headers, which is
 * the only level the redaction below can be observed at.
 */
internal fun tmdbHttpClient(
    config: TmdbConfig,
    engine: HttpClientEngine,
    logger: Logger = Logger.DEFAULT,
    logLevel: LogLevel = LogLevel.INFO,
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
            this.logger = logger
            level = logLevel
            // The production level logs no header at all, so this is what holds if the level is
            // ever raised: the credential is on every request, and logging is the one place that
            // would otherwise write it out.
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        defaultRequest {
            url(config.baseUrl)
            header(HttpHeaders.Authorization, BEARER_PREFIX + config.readAccessToken)
        }
    }
