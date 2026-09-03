package com.shayan.amro.core.network.sources.tmdb.mapper

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.network.sources.tmdb.TMDB_SOURCE
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMovieDetailDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbMovieDto
import com.shayan.amro.core.network.sources.tmdb.dto.TmdbTrendingPageDto
import com.shayan.amro.core.network.sources.tmdb.testJson
import com.shayan.amro.core.testing.TmdbFixture
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private const val COMPLETE_TITLE = "Project Hail Mary"
private const val COMPLETE_ID = "687163"
private const val COMPLETE_TAGLINE = "Believe in the Hail Mary."
private const val COMPLETE_IMDB_ID = "tt12042730"
private const val COMPLETE_RUNTIME_MINUTES = 157
private val COMPLETE_RELEASE_DATE = LocalDate(2026, 3, 15)

/** What every built record is identified and titled by, since no rule under test turns on either. */
private const val ROW_ID = 11L
private const val ROW_TITLE = "A row"

private const val ROW_POPULARITY = 42.5

/** The smallest score seen across the recorded pages, so no real row sorts below an unscored one. */
private const val LOWEST_TMDB_SCORE = 2.1233

/** A row TMDB has never sent, since it scores every one it serves. */
private const val ROW_WITHOUT_POPULARITY = """{"id":$ROW_ID,"title":"$ROW_TITLE"}"""

/** An id TMDB does not issue, so a row carrying it has a genre the table has no entry for. */
private const val UNKNOWN_GENRE_ID = 999_999

/** A date string TMDB would never send, and the empty one it sends for a movie with no date. */
private const val UNPARSEABLE_DATE = "soon"
private const val EMPTY_DATE = ""

private const val SCORE = 7.5
private const val VOTES = 120

/** The first row of the recorded trending page, which carries every field a row can. */
private const val RECORDED_ROW_ID = "1386315"
private const val RECORDED_ROW_TITLE = "The Runner"
private const val RECORDED_ROW_POPULARITY = 161.6957
private val RECORDED_ROW_RELEASE_DATE = LocalDate(2026, 9, 3)

class TmdbMovieMapperTest {
    @Test
    fun `the complete record maps every field TMDB populated`() {
        val detail = recorded(TmdbFixture.RELEASED_MOVIE).toMovieDetail()

        assertEquals(TMDB_SOURCE, detail.movie.id.source)
        assertEquals(COMPLETE_ID, detail.movie.id.value)
        assertEquals(COMPLETE_TITLE, detail.movie.title)
        assertEquals(listOf(Genre.SCIENCE_FICTION, Genre.ADVENTURE), detail.movie.genres)
        assertEquals(COMPLETE_RELEASE_DATE, detail.movie.releaseDate)
        assertNotNull(detail.movie.poster)
        assertEquals(COMPLETE_TAGLINE, detail.tagline)
        assertNotNull(detail.overview)
        assertNotNull(detail.backdrop)
        assertEquals(COMPLETE_RUNTIME_MINUTES.minutes, detail.runtime)
        assertEquals(ReleaseStatus.RELEASED, detail.status)
        assertNotNull(detail.budget)
        assertNotNull(detail.revenue)
        assertNotNull(detail.rating)
        assertEquals(COMPLETE_IMDB_ID, detail.imdbId)
    }

    @Test
    fun `the sparse record maps tagline budget revenue and rating to null`() {
        val detail = recorded(TmdbFixture.IN_PRODUCTION_MOVIE).toMovieDetail()

        assertNull(detail.tagline)
        assertNull(detail.budget)
        assertNull(detail.revenue)
        assertNull(detail.rating)
    }

    @Test
    fun `the unreleased record maps to post production with no rating`() {
        val detail = recorded(TmdbFixture.POST_PRODUCTION_MOVIE).toMovieDetail()

        assertEquals(ReleaseStatus.POST_PRODUCTION, detail.status)
        assertNull(detail.rating)
    }

    @Test
    fun `a record carrying nothing beyond its id and title maps every absence to null`() {
        val detail = detail().toMovieDetail()

        assertNull(detail.overview)
        assertNull(detail.tagline)
        assertNull(detail.backdrop)
        assertNull(detail.movie.poster)
        assertNull(detail.movie.releaseDate)
        assertNull(detail.runtime)
        assertNull(detail.budget)
        assertNull(detail.revenue)
        assertNull(detail.rating)
        assertNull(detail.imdbId)
        assertEquals(ReleaseStatus.UNKNOWN, detail.status)
    }

    @Test
    fun `a score with votes maps to a rating carrying its count`() {
        val rating = detail(voteAverage = SCORE, voteCount = VOTES).toMovieDetail().rating

        assertEquals(SCORE, rating?.average)
        assertEquals(VOTES, rating?.count)
    }

    @Test
    fun `a score with no votes maps to a rating with no count`() {
        val rating = detail(voteAverage = SCORE).toMovieDetail().rating

        assertEquals(SCORE, rating?.average)
        assertNull(rating?.count)
    }

    @Test
    fun `no score and no votes maps to no rating at all`() {
        assertNull(detail().toMovieDetail().rating)
    }

    @Test
    fun `a row orders by the score TMDB sent and nothing else`() {
        assertEquals(ROW_POPULARITY, row(popularity = ROW_POPULARITY).toMovie().popularity)
    }

    @Test
    fun `a row with no score still becomes a movie and sorts below every scored one`() {
        val unscored = testJson.decodeFromString<TmdbMovieDto>(ROW_WITHOUT_POPULARITY).toMovie()

        assertEquals(ROW_TITLE, unscored.title, "the row is kept rather than failing its page")
        assertTrue(unscored.popularity < row(popularity = LOWEST_TMDB_SCORE).toMovie().popularity)
    }

    @Test
    fun `an unparseable date maps to no date rather than a failure`() {
        assertNull(row(releaseDate = UNPARSEABLE_DATE).toMovie().releaseDate)
        assertNull(row(releaseDate = EMPTY_DATE).toMovie().releaseDate)
    }

    @Test
    fun `a genre the table has no entry for is dropped`() {
        val movie = row(genreIds = listOf(UNKNOWN_GENRE_ID)).toMovie()

        assertEquals(emptyList(), movie.genres)
    }

    @Test
    fun `a recorded row arrives with every field TMDB names differently`() {
        val movie = recordedRow().toMovie()

        assertEquals(RECORDED_ROW_ID, movie.id.value)
        assertEquals(RECORDED_ROW_TITLE, movie.title)
        assertEquals(listOf(Genre.THRILLER), movie.genres)
        assertEquals(RECORDED_ROW_RELEASE_DATE, movie.releaseDate)
        assertEquals(RECORDED_ROW_POPULARITY, movie.popularity)
        assertNotNull(movie.poster)
    }
}

/** A recorded response, decoded the way the client decodes it. */
private fun recorded(fixture: TmdbFixture) =
    testJson.decodeFromString<TmdbMovieDetailDto>(fixture.json)

/**
 * The first row of a recorded trending page, decoded the way the client decodes it.
 *
 * A built row cannot catch a wire name that stops matching, because it never travels over the wire.
 * This one does, so a renamed field reaches the assertions as an absence.
 */
private fun recordedRow() =
    testJson
        .decodeFromString<TmdbTrendingPageDto>(TmdbFixture.TRENDING_PAGE_1.json)
        .results
        .first()

/**
 * A list row carrying only the field the rule under test turns on.
 *
 * Every other field keeps the value an absent one decodes to, so a test states its own case and
 * nothing else.
 */
private fun row(
    popularity: Double? = ROW_POPULARITY,
    releaseDate: String? = null,
    genreIds: List<Int> = emptyList(),
) = TmdbMovieDto(
    id = ROW_ID,
    title = ROW_TITLE,
    genreIds = genreIds,
    popularity = popularity,
    releaseDate = releaseDate,
)

/** A detail record carrying only the score and count the rule under test turns on. */
private fun detail(
    voteAverage: Double = 0.0,
    voteCount: Int = 0,
) = TmdbMovieDetailDto(
    id = ROW_ID,
    title = ROW_TITLE,
    popularity = ROW_POPULARITY,
    voteAverage = voteAverage,
    voteCount = voteCount,
)
