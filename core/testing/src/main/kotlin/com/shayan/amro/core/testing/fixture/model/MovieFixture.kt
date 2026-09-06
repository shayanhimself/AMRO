package com.shayan.amro.core.testing.fixture.model

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.ImageRef
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** The fixture of Movie models. */
object MovieFixture {
    /**
     * A movie carrying every field a source can fill.
     */
    fun movie(
        id: String = "1294189",
        title: String = "The Mongoose",
        genres: List<Genre> = listOf(Genre.HORROR, Genre.THRILLER),
        popularity: Double = 12.5,
        releaseDate: LocalDate? = LocalDate.parse("2026-05-14"),
        poster: ImageRef? =
            ImageRef(
                small = "https://image.example/w185/mongoose.jpg",
                large = "https://image.example/w500/mongoose.jpg",
            ),
    ): Movie =
        Movie(
            id = id,
            title = title,
            genres = genres,
            popularity = popularity,
            releaseDate = releaseDate,
            poster = poster,
        )

    /** The fuller record behind one [movie], carrying every field a source can fill. */
    fun detail(
        movie: Movie = movie(),
        overview: String? = "A mongoose walks into a bar.",
        tagline: String? = "It bites.",
        backdrop: ImageRef? =
            ImageRef(
                small = "https://image.example/w300/mongoose.jpg",
                large = "https://image.example/w1280/mongoose.jpg",
            ),
        runtime: Duration? = 107.minutes,
        status: ReleaseStatus = ReleaseStatus.RELEASED,
        budget: Long? = 90_000_000,
        revenue: Long? = 12_000_000,
        rating: Rating? = Rating(average = 7.4, count = 1284),
        imdbId: String? = "tt1234567",
    ): MovieDetail =
        MovieDetail(
            movie = movie,
            overview = overview,
            tagline = tagline,
            backdrop = backdrop,
            runtime = runtime,
            status = status,
            budget = budget,
            revenue = revenue,
            rating = rating,
            imdbId = imdbId,
        )

    /** One movie, where nothing about it decides the test. */
    val MOVIE = movie()

    /** A second movie, differing from [MOVIE] in every field a test reads back. */
    val OTHER_MOVIE =
        movie(
            id = "755898",
            title = "War of the Worlds",
            genres = listOf(Genre.SCIENCE_FICTION),
            popularity = 3.5,
            releaseDate = LocalDate.parse("2005-06-13"),
        )

    /** An accented title, and the most popular and oldest movie of the set. */
    val MOVIE_WITH_ACCENTED_TITLE =
        movie(
            id = "1",
            title = "Amélie",
            genres = listOf(Genre.ROMANCE, Genre.COMEDY),
            popularity = 30.0,
            releaseDate = LocalDate.parse("2001-04-25"),
        )

    /**
     * The same title without its accent, which only a collator orders beside
     * [MOVIE_WITH_ACCENTED_TITLE].
     */
    val MOVIE_WITH_UNACCENTED_TITLE =
        movie(
            id = "2",
            title = "Amelie",
            genres = listOf(Genre.ROMANCE),
            popularity = 20.0,
            releaseDate = LocalDate.parse("2019-01-01"),
        )

    /** A lowercase title, tying popularity and release date with [MOVIE_WITH_CAPITALISED_TITLE]. */
    val MOVIE_WITH_LOWERCASE_TITLE =
        movie(
            id = "3",
            title = "boyhood",
            genres = listOf(Genre.DRAMA),
            popularity = 10.0,
            releaseDate = LocalDate.parse("2014-07-11"),
        )

    /** The same title cased, so every key it carries ties with [MOVIE_WITH_LOWERCASE_TITLE]. */
    val MOVIE_WITH_CAPITALISED_TITLE =
        movie(
            id = "4",
            title = "Boyhood",
            genres = listOf(Genre.DRAMA, Genre.COMEDY),
            popularity = 10.0,
            releaseDate = LocalDate.parse("2014-07-11"),
        )

    /**
     * The newest release, tying popularity with [MOVIE_WITH_UNACCENTED_TITLE] and sharing a genre
     * with nothing.
     */
    val MOVIE_WITH_NEWEST_RELEASE =
        movie(
            id = "5",
            title = "Dune",
            genres = listOf(Genre.SCIENCE_FICTION, Genre.ADVENTURE),
            popularity = 20.0,
            releaseDate = LocalDate.parse("2021-09-15"),
        )

    /** A movie with no release date, which every date ordering places last. */
    val MOVIE_WITH_NO_RELEASE_DATE =
        movie(
            id = "6",
            title = "Zodiac",
            genres = listOf(Genre.CRIME, Genre.THRILLER),
            popularity = 5.0,
            releaseDate = null,
        )

    val MOVIES_IN_NO_ORDER =
        listOf(
            MOVIE_WITH_NO_RELEASE_DATE,
            MOVIE_WITH_ACCENTED_TITLE,
            MOVIE_WITH_CAPITALISED_TITLE,
            MOVIE_WITH_NEWEST_RELEASE,
            MOVIE_WITH_UNACCENTED_TITLE,
            MOVIE_WITH_LOWERCASE_TITLE,
        )
}
