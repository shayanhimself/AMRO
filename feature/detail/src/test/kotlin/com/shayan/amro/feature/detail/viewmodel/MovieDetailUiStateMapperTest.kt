package com.shayan.amro.feature.detail.viewmodel

import androidx.annotation.StringRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.ui.label.labelRes
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.detail.R
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val IMDB_TITLE_URL = "https://www.imdb.com/title/"

@RunWith(AndroidJUnit4::class)
class MovieDetailUiStateMapperTest {
    @Test
    fun `a movie with no rating states its status in the slot, and leaves the grid`() {
        val content = loaded(MovieFixture.detail(status = UNRELEASED, rating = null))

        assertEquals(
            HeaderBadgeUiState.Status(
                AmroText.Resource(R.string.feature_detail_status_in_production),
            ),
            content.badge,
        )
        assertNull(content.facts.fact(R.string.feature_detail_fact_status))
    }

    @Test
    fun `a movie with a rating closes the grid with its status`() {
        val content = loaded(MovieFixture.detail())

        assertTrue(content.badge is HeaderBadgeUiState.Rating, "expected a rating")
        assertEquals(
            fact(
                labelRes = R.string.feature_detail_fact_status,
                value = AmroText.Resource(R.string.feature_detail_status_released),
            ),
            content.facts.last(),
        )
    }

    @Test
    fun `the grid reads in the order a reader last saw it in`() {
        val facts = loaded(MovieFixture.detail()).facts

        assertEquals(
            listOf(
                R.string.feature_detail_fact_runtime,
                R.string.feature_detail_fact_release_date,
                R.string.feature_detail_fact_budget,
                R.string.feature_detail_fact_revenue,
                R.string.feature_detail_fact_status,
            ).map { AmroText.Resource(it) },
            facts.map { it.label },
        )
    }

    @Test
    fun `a rating with no vote count is the rating alone`() {
        val rating = Rating(average = 7.4, count = null)

        val badge = loaded(MovieFixture.detail(rating = rating)).badge

        assertTrue(badge is HeaderBadgeUiState.Rating, "expected a rating, was $badge")
        assertEquals(ratingText(rating.average), badge.rating)
        assertNull(badge.voteCount)
    }

    @Test
    fun `budget, revenue and runtime with no value read as undisclosed`() {
        val facts =
            loaded(MovieFixture.detail(budget = null, revenue = null, runtime = null)).facts

        listOf(
            R.string.feature_detail_fact_runtime,
            R.string.feature_detail_fact_budget,
            R.string.feature_detail_fact_revenue,
        ).forEach { labelRes ->
            val cell = checkNotNull(facts.fact(labelRes))
            assertFalse(cell.disclosed, "expected undisclosed, was $cell")
            assertEquals(AmroText.Resource(R.string.feature_detail_not_disclosed), cell.value)
        }
    }

    @Test
    fun `an absent tagline and an absent IMDB id leave no tagline and no action`() {
        val content = loaded(MovieFixture.detail(tagline = "", imdbId = null))

        assertNull(content.tagline)
        assertNull(content.imdbUrl)
    }

    @Test
    fun `the missing-data record is a status pill over the four fixed facts`() {
        val content = loaded(MISSING_DATA)

        assertTrue(content.badge is HeaderBadgeUiState.Status, "expected a status pill")
        assertNull(content.tagline)
        assertNull(content.facts.fact(R.string.feature_detail_fact_status))
        assertEquals(4, content.facts.size)
        assertEquals(
            checkNotNull(MISSING_DATA.movie.releaseDate).toDateText(),
            content.facts.fact(R.string.feature_detail_fact_release_date)?.value,
        )
    }

    @Test
    fun `a first load carries the poster and the genres the trending row handed over`() {
        val movie = MovieFixture.detail().movie

        val content = skeleton(movie)

        val labels: List<AmroText> = movie.genres.map { AmroText.Resource(it.labelRes) }

        assertEquals(movie.poster?.small, content.posterUrl)
        assertEquals(labels, content.genreLabels.toList())
    }

    @Test
    fun `a first load with no summary carries nothing to draw`() {
        val content = skeleton(movie = null)

        assertNull(content.posterUrl)
        assertTrue(content.genreLabels.isEmpty(), "expected no genres")
    }

    @Test
    fun `an IMDB id becomes the title's address`() {
        val detail = MovieFixture.detail()

        val url = loaded(detail).imdbUrl

        assertEquals(IMDB_TITLE_URL + detail.imdbId, url)
    }
}

/** The status of a movie that has no votes and no box office yet. */
private val UNRELEASED = ReleaseStatus.IN_PRODUCTION

/** The shape two in five records take: unreleased, with six of the thirteen fields empty. */
private val MISSING_DATA =
    MovieFixture.detail(
        tagline = "",
        status = UNRELEASED,
        budget = null,
        revenue = null,
        rating = null,
    )

/**
 * Movie detail loaded state.
 */
private fun loaded(detail: MovieDetail): MovieDetailContent.Loaded {
    val content = toMovieDetailUiState(movie = detail.movie, detail = detail, error = null).content
    assertTrue(content is MovieDetailContent.Loaded, "expected loaded, was $content")
    return content
}

/**
 * Movie detail skeleton (loading) state.
 */
private fun skeleton(movie: Movie?): MovieDetailContent.Skeleton {
    val content = toMovieDetailUiState(movie = movie, detail = null, error = null).content
    assertTrue(content is MovieDetailContent.Skeleton, "expected a skeleton, was $content")
    return content
}

/**
 * Builds a movie fact.
 */
private fun List<MovieFactUiState>.fact(
    @StringRes labelRes: Int,
): MovieFactUiState? = firstOrNull { it.label == AmroText.Resource(labelRes) }
