package com.shayan.amro.core.testing.fixture.model

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.ImageRef
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.model.MovieId
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.model.SourceId
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** The fixture of Movie models. */
object MovieFixture {
    /**
     * A movie carrying every field a source can fill.
     */
    fun movie(
        id: MovieId = MovieId(SourceId("tmdb"), "1294189"),
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
}
