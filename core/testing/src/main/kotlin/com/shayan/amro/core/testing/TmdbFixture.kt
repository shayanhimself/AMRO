package com.shayan.amro.core.testing

/** Where the recordings sit in the source tree, which is what a reader opening this file wants. */
private const val TMDB_FIXTURE_DIRECTORY = "core/testing/src/main/resources/fixtures/tmdb/"

/** The same directory as the classpath sees it, which is what reads them at run time. */
private const val TMDB_FIXTURE_ROOT = "fixtures/tmdb/"

/** What re-records them. */
private const val RECORDER = "scripts/record-fixtures.py"

/**
 * A recorded TMDB response.
 *
 * A fixture is what the provider actually sent, so a test asserts against the shapes TMDB produces
 * rather than the ones the test's own author remembered to build.
 *
 * The files are checked in under `core/testing/src/main/resources/fixtures/tmdb/`, and
 * `scripts/record-fixtures.py` re-records them against the live API.
 */
enum class TmdbFixture(
    private val path: String,
) {
    /** The first six pages of the trending list, recorded in one sequence. */
    TRENDING_PAGE_1("trending/page-1.json"),
    TRENDING_PAGE_2("trending/page-2.json"),
    TRENDING_PAGE_3("trending/page-3.json"),
    TRENDING_PAGE_4("trending/page-4.json"),
    TRENDING_PAGE_5("trending/page-5.json"),
    TRENDING_PAGE_6("trending/page-6.json"),

    /**
     * Two pages assembled from recorded rows so that rows repeat across the boundary, which is what
     * TMDB re-ranking mid-sequence produces.
     */
    OVERLAP_PAGE_1("trending/overlap/page-1.json"),
    OVERLAP_PAGE_2("trending/overlap/page-2.json"),

    /** Every genre TMDB publishes, the evidence the genre table is total over what it sends. */
    GENRES("genres.json"),

    /** A released movie, with every field TMDB publishes populated. */
    RELEASED_MOVIE("movie/released.json"),

    /**
     * A movie still in production. Nothing has been disclosed and nobody has voted, so its tagline
     * is empty and its budget, revenue and votes are all zero.
     */
    IN_PRODUCTION_MOVIE("movie/in-production.json"),

    /**
     * A movie in post production. It holds the same absences as [IN_PRODUCTION_MOVIE] and exists
     * beside it because the two statuses are two entries in the status table.
     */
    POST_PRODUCTION_MOVIE("movie/post-production.json"),

    /** What TMDB answers a request carrying an invalid token with. */
    UNAUTHORISED_ERROR("error/401.json"),

    /** What TMDB answers a request for an id it does not issue with. */
    NOT_FOUND_ERROR("error/404.json"),
    ;

    /**
     * Reads the JSON files in resources. Returns the response body, exactly as TMDB sent it.
     *
     * @throws IllegalStateException naming the file that is missing, which is a recording renamed
     * or deleted rather than anything a provider did.
     */
    val json: String
        get() =
            classpathResource(TMDB_FIXTURE_ROOT + path)
                ?: error("no recording at $TMDB_FIXTURE_DIRECTORY$path. Re-record with $RECORDER")

    companion object {
        /**
         * The recorded trending pages in the order they were served, for a test that pages the way
         * the fetch policy does.
         */
        val TRENDING_PAGES =
            listOf(
                TRENDING_PAGE_1,
                TRENDING_PAGE_2,
                TRENDING_PAGE_3,
                TRENDING_PAGE_4,
                TRENDING_PAGE_5,
                TRENDING_PAGE_6,
            )
    }
}
