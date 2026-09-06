package com.shayan.amro.core.network.sources

private const val SOURCE_SEPARATOR = ":"

/**
 * One movie, as the source that issued it names it. Each source has their own movie id format.
 *
 * @property source the provider that issued [sourceMovieId].
 * @property sourceMovieId the movie id as that provider wrote it. Same [sourceMovieId] from two
 * different providers' stay two different movies.
 */
internal data class MovieId(
    val source: String,
    val sourceMovieId: String,
) {
    /**
     * How this movie is identified everywhere outside this module.
     *
     * The id space belongs to whoever issued it, so the source travels with it.
     */
    val qualified: String get() = source + SOURCE_SEPARATOR + sourceMovieId

    companion object {
        /**
         * Reads a qualified id back into the parts it was built from.
         *
         * @param qualified an id [MovieId.qualified] wrote.
         */
        fun of(qualified: String): MovieId =
            MovieId(
                source = qualified.substringBefore(SOURCE_SEPARATOR),
                sourceMovieId = qualified.substringAfter(SOURCE_SEPARATOR),
            )
    }
}
