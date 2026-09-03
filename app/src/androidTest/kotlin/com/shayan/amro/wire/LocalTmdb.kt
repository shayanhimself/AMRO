package com.shayan.amro.wire

import com.shayan.amro.core.network.sources.tmdb.TmdbConfig
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.Protocol
import org.junit.rules.ExternalResource
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val JSON_CONTENT_TYPE = "application/json"
private const val OK = 200
private const val SERVER_ERROR = 500
private const val REPLY_WAIT_SECONDS = 2L
private const val REQUEST_TIMEOUT_SECONDS = 10L

/** The credential the app sends while this server is answering. Any non-empty value will do. */
private const val TEST_READ_ACCESS_TOKEN = "test-read-access-token"

/**
 * Answers as TMDB for the duration of one instrumented test.
 *
 * The address the app resolves its endpoints against is an injected value, so a test binds
 * [config] in place of the app's own and the requests arrive here. Everything below that binding
 * is the app's: its Ktor client, its OkHttp engine, its parsing.
 *
 * The server is `mockwebserver3`, the artifact that carries OkHttp 5's server.
 */
class LocalTmdb : ExternalResource() {
    private val dispatcher = QueuedDispatcher()

    private val server =
        MockWebServer().apply {
            this.dispatcher = this@LocalTmdb.dispatcher
            // HTTP/1.1 only, so that dropping a response part-way through is a socket closing
            // under the client rather than a stream reset the framing layer reports for it.
            protocols = listOf(Protocol.HTTP_1_1)
            start()
        }

    /**
     * The address and credential to bind in place of the app's own.
     *
     * The server is started above rather than in [before] because this is read while the test
     * class is constructed, which a Hilt `@BindValue` field is, before any rule runs.
     */
    val config: TmdbConfig =
        TmdbConfig(
            baseUrl = server.url("/").toString(),
            readAccessToken = TEST_READ_ACCESS_TOKEN,
        )

    override fun after() {
        server.close()
    }

    /** Answers the next request with [body] as a successful JSON response. */
    fun enqueueJson(body: String) {
        dispatcher.enqueue(
            MockResponse
                .Builder()
                .code(OK)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .body(body)
                .build(),
        )
    }

    /** Answers the next request with [code] and no body. */
    fun enqueueStatus(code: Int) {
        dispatcher.enqueue(MockResponse.Builder().code(code).build())
    }

    /**
     * Starts a response carrying [body] and drops the connection part-way through it, which is the
     * failure a mocked engine cannot produce: the app sees a real socket close mid-response.
     */
    fun enqueueDisconnectMidResponse(body: String) {
        dispatcher.enqueue(
            MockResponse
                .Builder()
                .code(OK)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .body(body)
                .onResponseBody(SocketEffect.CloseSocket())
                .build(),
        )
    }

    /** The next request the app sent, for asserting path, headers and body. */
    fun lastRequest(): RecordedRequest =
        server.takeRequest(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            ?: error("no request reached the server within $REQUEST_TIMEOUT_SECONDS seconds")
}

/** Serves the enqueued responses in the order they were enqueued. */
private class QueuedDispatcher : Dispatcher() {
    private val replies = LinkedBlockingQueue<MockResponse>()

    fun enqueue(response: MockResponse) {
        replies.add(response)
    }

    override fun peek(): MockResponse = replies.peek() ?: MockResponse()

    // Waiting rather than taking, so a request that outruns the response enqueued for it still
    // finds it, and a request with no response at all ends as a failed call instead of holding a
    // connection open past the server's own shutdown grace.
    override fun dispatch(request: RecordedRequest): MockResponse =
        replies.poll(REPLY_WAIT_SECONDS, TimeUnit.SECONDS) ?: NO_REPLY_RESPONSE

    private companion object {
        val NO_REPLY_RESPONSE: MockResponse = MockResponse.Builder().code(SERVER_ERROR).build()
    }
}
