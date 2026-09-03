package com.shayan.amro.core.testing.fixture.tmdb

/** Every recording is a JSON body, so no entry spells its own extension. */
private const val FIXTURE_EXTENSION = ".json"

/** The recordings, grouped the way the fixture tree is. */
object TmdbFixture {
    /** The head of the trending list, recorded in one sequence. */
    enum class Trending(
        private val file: String,
    ) : TmdbRecording {
        PAGE_1("page-1"),
        PAGE_2("page-2"),
        PAGE_3("page-3"),
        PAGE_4("page-4"),
        PAGE_5("page-5"),
        PAGE_6("page-6"),
        ;

        override val path get() = "trending/$file$FIXTURE_EXTENSION"
    }

    /**
     * Pages assembled from recorded rows so that rows repeat across the boundary, which is what
     * TMDB re-ranking mid-sequence produces.
     */
    enum class Overlap(
        private val file: String,
    ) : TmdbRecording {
        PAGE_1("page-1"),
        PAGE_2("page-2"),
        ;

        override val path get() = "trending/overlap/$file$FIXTURE_EXTENSION"
    }

    /** A single movie's record, one per release status the detail screen renders. */
    enum class Movie(
        private val file: String,
    ) : TmdbRecording {
        /** Every field TMDB publishes, populated. */
        RELEASED("released"),

        /**
         * A movie still in production. Nothing has been disclosed and nobody has voted, so its
         * tagline is empty and its budget, revenue and votes are all zero.
         */
        IN_PRODUCTION("in-production"),

        /**
         * A movie in post production. It holds the same absences as [IN_PRODUCTION] and exists
         * beside it because the two statuses are two entries in the status table.
         */
        POST_PRODUCTION("post-production"),
        ;

        override val path get() = "movie/$file$FIXTURE_EXTENSION"
    }

    /** What TMDB answers a request it refuses with, named by the status it answers under. */
    enum class Error(
        private val file: String,
    ) : TmdbRecording {
        /** A request carrying an invalid token. */
        UNAUTHORISED("401"),

        /** A request for an id TMDB does not issue. */
        NOT_FOUND("404"),
        ;

        override val path get() = "error/$file$FIXTURE_EXTENSION"
    }

    /** Every genre TMDB publishes, the evidence the genre table is total over what it sends. */
    object Genres : TmdbRecording {
        override val path = "genres$FIXTURE_EXTENSION"
    }

    /**
     * Every recording there is.
     *
     * A group added here is a group the packaging test starts covering, which is why that test
     * reads this rather than any one group.
     */
    val ALL: List<TmdbRecording> =
        Trending.entries + Overlap.entries + Movie.entries + Error.entries + Genres
}
