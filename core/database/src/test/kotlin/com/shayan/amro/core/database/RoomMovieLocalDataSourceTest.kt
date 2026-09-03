package com.shayan.amro.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.SourceId
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import com.shayan.amro.core.testing.fixture.model.MovieFixture.movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** What the same movie's tagline reads as once TMDB has been asked again. */
private const val SECOND_TAGLINE = "It bites twice."

/** How long a test waits for Room to publish a change before it calls the emission missing. */
private val EMISSION_TIMEOUT = 5.seconds

/** How long a test waits after the emission it expected, to see whether another follows. */
private val QUIET_PERIOD = 250.milliseconds

@RunWith(RobolectricTestRunner::class)
class RoomMovieLocalDataSourceTest {
    private lateinit var database: AmroDatabase
    private lateinit var source: MovieLocalDataSource

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AmroDatabase::class.java,
                ).setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        source = RoomMovieLocalDataSource(database.movieDao(), database.movieDetailDao())
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun `a replace emits once with the new set and never an empty list`() =
        runBlocking {
            val emissions = Channel<List<Movie>>(Channel.UNLIMITED)
            val collector =
                launch(Dispatchers.IO) { source.getTrendingFlow().collect(emissions::send) }

            assertEquals(emptyList(), emissions.next())

            source.replaceTrending(listOf(FIRST_MOVIE, SECOND_MOVIE))

            assertEquals(listOf(FIRST_MOVIE, SECOND_MOVIE), emissions.next().sortedBy { it.title })
            assertNull(emissions.nextOrNull(), "the write published more than one set")

            collector.cancel()
        }

    @Test
    fun `a replace removes the rows the new set does not contain`() =
        runBlocking {
            source.replaceTrending(listOf(FIRST_MOVIE, SECOND_MOVIE))

            source.replaceTrending(listOf(SECOND_MOVIE, THIRD_MOVIE))

            assertEquals(
                listOf(SECOND_MOVIE, THIRD_MOVIE),
                source.getTrendingFlow().first().sortedBy { it.title },
            )
        }

    @Test
    fun `a detail written for a movie the trending table does not hold still reads back`() =
        runBlocking {
            source.writeMovieDetail(FIRST_DETAIL)

            source.replaceTrending(listOf(SECOND_MOVIE))

            assertEquals(FIRST_DETAIL, source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `writing a detail leaves the trending table untouched`() =
        runBlocking {
            source.replaceTrending(listOf(SECOND_MOVIE))

            source.writeMovieDetail(FIRST_DETAIL)

            assertEquals(listOf(SECOND_MOVIE), source.getTrendingFlow().first())
        }

    @Test
    fun `a detail read with nothing cached emits null`() =
        runBlocking {
            assertNull(source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a movie read for one a refresh dropped emits null`() =
        runBlocking {
            source.replaceTrending(listOf(FIRST_MOVIE))

            source.replaceTrending(listOf(SECOND_MOVIE))

            assertNull(source.getMovieFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a movie read for one the trending set holds emits it`() =
        runBlocking {
            source.replaceTrending(listOf(FIRST_MOVIE, SECOND_MOVIE))

            assertEquals(FIRST_MOVIE, source.getMovieFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `a detail write reaches a subscriber already collecting`() =
        runBlocking {
            val emissions = Channel<MovieDetail?>(Channel.UNLIMITED)
            val collector =
                launch(Dispatchers.IO) {
                    source.getMovieDetailFlow(FIRST_MOVIE.id).collect(emissions::send)
                }
            assertNull(emissions.next())

            source.writeMovieDetail(FIRST_DETAIL)

            assertEquals(FIRST_DETAIL, emissions.next())

            val refetched = FIRST_DETAIL.copy(tagline = SECOND_TAGLINE)
            source.writeMovieDetail(refetched)

            assertEquals(refetched, emissions.next())

            collector.cancel()
        }

    @Test
    fun `writing a detail twice leaves one row, carrying the second`() =
        runBlocking {
            val refetched = FIRST_DETAIL.copy(tagline = SECOND_TAGLINE)
            source.writeMovieDetail(FIRST_DETAIL)

            source.writeMovieDetail(refetched)

            assertEquals(refetched, source.getMovieDetailFlow(FIRST_MOVIE.id).first())
        }

    @Test
    fun `two sources issuing one id are two movies`() =
        runBlocking {
            val other = FIRST_MOVIE.copy(id = FIRST_MOVIE.id.copy(source = SourceId("omdb")))

            source.replaceTrending(listOf(FIRST_MOVIE, other))
            source.writeMovieDetail(FIRST_DETAIL)

            assertEquals(2, source.getTrendingFlow().first().size)
            assertNull(source.getMovieDetailFlow(other.id).first())
        }
}

private suspend fun <T> Channel<T>.next(): T = withTimeout(EMISSION_TIMEOUT) { receive() }

private suspend fun <T> Channel<T>.nextOrNull(): T? = withTimeoutOrNull(QUIET_PERIOD) { receive() }

/**
 * Three movies of one trending set, titled so that this order is also their title order, which is
 * what the tests that sort by title assert against.
 */
private val FIRST_MOVIE = movie(title = "The Mongoose")

private val SECOND_MOVIE =
    movie(
        id = FIRST_MOVIE.id.copy(value = "755898"),
        title = "War of the Worlds",
    )

private val THIRD_MOVIE =
    movie(
        id = FIRST_MOVIE.id.copy(value = "1078605"),
        title = "Weapons",
    )

private val FIRST_DETAIL = detail(movie = FIRST_MOVIE)
