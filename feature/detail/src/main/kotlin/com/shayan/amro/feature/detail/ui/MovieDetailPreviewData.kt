package com.shayan.amro.feature.detail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.ImageRef
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.detail.viewmodel.MovieDetailContent
import com.shayan.amro.feature.detail.viewmodel.MovieDetailUiState
import com.shayan.amro.feature.detail.viewmodel.MovieFactUiState
import com.shayan.amro.feature.detail.viewmodel.toMovieDetailUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** What every preview image is painted as, so a golden captures layout and never a download. */
private val PREVIEW_ART_COLOR = Color(0xFF55617A)

/** A record with every field a source can fill. */
private val COMPLETE =
    previewDetail(
        movie =
            previewMovie(
                id = "1",
                title = "Project Hail Mary",
                genres = listOf(Genre.SCIENCE_FICTION, Genre.ADVENTURE),
                releaseDate = LocalDate.parse("2026-03-15"),
            ),
        overview =
            "Science teacher Ryland Grace wakes up on a spaceship light years from home with no " +
                "recollection of who he is or how he got there. As his memory returns, he begins " +
                "to uncover his mission: solve the riddle of the mysterious substance causing " +
                "the sun to die out.",
        tagline = "Believe in the Hail Mary.",
        runtime = 157.minutes,
        budget = 200_000_000,
        revenue = 684_234_722,
        rating = Rating(average = 8.646, count = 7472),
    )

/**
 * No tagline, no votes and no box office.
 */
private val MISSING =
    previewDetail(
        movie =
            previewMovie(
                id = "2",
                title = "The Mongoose",
                genres = listOf(Genre.ACTION, Genre.THRILLER),
                releaseDate = LocalDate.parse("2026-10-30"),
            ),
        overview =
            "A falsely accused war hero with nothing to lose leads police on an epic televised " +
                "cross-country car chase, helped by members of his former Special Forces Army " +
                "battalion.",
        tagline = null,
        runtime = 99.minutes,
        status = ReleaseStatus.IN_PRODUCTION,
        budget = null,
        revenue = null,
        rating = null,
    )

/**
 * Six genres, a ten-digit revenue, longest title and overview in the sample.
 */
private val CROWDED =
    previewDetail(
        movie =
            previewMovie(
                id = "3",
                title = "MOBILE SUIT GUNDAM HATHAWAY The Sorcery of Nymph Circe",
                genres =
                    listOf(
                        Genre.ANIMATION,
                        Genre.ACTION,
                        Genre.ADVENTURE,
                        Genre.SCIENCE_FICTION,
                        Genre.CRIME,
                        Genre.DRAMA,
                    ),
                releaseDate = LocalDate.parse("2026-01-30"),
            ),
        overview =
            "Haunted by past trauma, Hathaway Noa is drawn to a mysterious girl named Gigi " +
                "Andalucia, whose strange powers stir memories within him. While swayed by her " +
                "cryptic words, he continues preparing for Mafty's mission: the attack on the " +
                "Adelaide Conference. Meanwhile, Kenneth Sleg of the Earth Federation Forces " +
                "prepares a defense operation and a plan to eliminate Mafty, and is approached " +
                "by Handley Yoxon of the Criminal Police Organization with a secret proposal.",
        tagline = null,
        runtime = 115.minutes,
        budget = null,
        revenue = 2_332_821_029,
        rating = Rating(average = 6.8, count = 16),
    )

/**
 * The states the movie detail screen is previewed and captured in.
 */
internal object MovieDetailPreviewData {
    /** A first load: the trending set names the screen, and the record has not arrived. */
    val SKELETON = previewState(detail = null)

    /** A first load on a movie no trending row was read for, which has nothing to draw. */
    val SKELETON_WITHOUT_SUMMARY = toMovieDetailUiState(movie = null, detail = null, error = null)

    /** The record, complete. */
    val LOADED = previewState()

    /** The record two in five movies actually have. */
    val MISSING_DATA = previewState(detail = MISSING)

    /** Six genres and a ten-digit revenue, under the longest title and overview. */
    val CROWDED_RECORD = previewState(detail = CROWDED)

    /** A movie never opened before, whose fetch failed. */
    val ERROR = previewState(detail = null, error = DataError.NoConnectivity)

    /** The film a first load already names, which the trending row handed over. */
    val SKELETON_TITLE = SKELETON.title.orEmpty()

    /** What a first load draws, which only a skeleton state has. */
    val SKELETON_CONTENT = SKELETON.content as MovieDetailContent.Skeleton

    /** The grid a complete record fills, closed by its status. */
    val FACTS = LOADED.facts

    /** The grid a record with no box office fills, whose status the header slot is carrying. */
    val UNDISCLOSED_FACTS = MISSING_DATA.facts

    /** The chips a complete record fills. */
    val GENRES = LOADED.genres
}

private val MovieDetailUiState.facts: ImmutableList<MovieFactUiState>
    get() = (content as MovieDetailContent.Loaded).facts

private val MovieDetailUiState.genres: ImmutableList<AmroText>
    get() = (content as? MovieDetailContent.Loaded)?.genreLabels ?: persistentListOf()

/**
 * Reads one state the way the screen is fed it.
 *
 * @param detail the record, or null where nothing is cached.
 * @param error the cause the last fetch failed with, or null.
 * @return a [MovieDetailUiState].
 */
private fun previewState(
    detail: MovieDetail? = COMPLETE,
    error: DataError? = null,
): MovieDetailUiState =
    toMovieDetailUiState(
        movie = detail?.movie ?: COMPLETE.movie,
        detail = detail,
        error = error,
    )

/**
 * Builds one movie the header renders.
 *
 * @param id what tells this movie from the others.
 * @param title the words the header and the bar both read.
 * @param genres what the chips list.
 * @param releaseDate the day the movie was released.
 */
private fun previewMovie(
    id: String,
    title: String,
    genres: List<Genre>,
    releaseDate: LocalDate,
): Movie =
    Movie(
        id = id,
        title = title,
        genres = genres,
        popularity = 0.0,
        releaseDate = releaseDate,
        poster = ImageRef(small = "preview://poster/$id", large = "preview://poster/$id"),
    )

/**
 * Builds one record the screen renders.
 *
 * @param movie the fields the trending list already holds.
 */
private fun previewDetail(
    movie: Movie,
    overview: String,
    tagline: String?,
    runtime: Duration,
    budget: Long?,
    revenue: Long?,
    rating: Rating?,
    status: ReleaseStatus = ReleaseStatus.RELEASED,
): MovieDetail =
    MovieDetail(
        movie = movie,
        overview = overview,
        tagline = tagline,
        backdrop =
            ImageRef(
                small = "preview://backdrop/${movie.id}",
                large = "preview://backdrop/${movie.id}",
            ),
        runtime = runtime,
        status = status,
        budget = budget,
        revenue = revenue,
        rating = rating,
        imdbId = "tt1234567",
    )

/**
 * Renders the screen the way a preview and a golden need it: every image painted locally, so
 * nothing on screen waits on a network.
 *
 * @param state what the screen renders.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun MovieDetailScreenPreviewHost(state: MovieDetailUiState) {
    val previewHandler = AsyncImagePreviewHandler { ColorImage(PREVIEW_ART_COLOR.toArgb()) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        MovieDetailScreen(
            uiState = state,
            onRetry = {},
            onOpenImdb = {},
            onBack = {},
        )
    }
}
