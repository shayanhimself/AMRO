package com.shayan.amro.core.network.sources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** One provider, named the way a source names itself. */
private const val ONE_SOURCE = "tmdb"

/** A second provider, which numbers its movies from one exactly as the first does. */
private const val ANOTHER_SOURCE = "trakt"

/** The id both providers issue for a movie of their own. */
private const val SHARED_MOVIE_ID = "1"

/** An id a provider wrote with the separator the grammar splits on. */
private const val MOVIE_ID_CARRYING_A_SEPARATOR = "movie:278"

class MovieIdTest {
    @Test
    fun `two sources issuing one id are two different movies`() {
        val one = MovieId(ONE_SOURCE, SHARED_MOVIE_ID)
        val other = MovieId(ANOTHER_SOURCE, SHARED_MOVIE_ID)

        assertNotEquals(one.qualified, other.qualified)
    }

    @Test
    fun `an id survives a round trip through its qualified form`() {
        val id = MovieId(ONE_SOURCE, SHARED_MOVIE_ID)

        assertEquals(id, MovieId.of(id.qualified))
    }

    @Test
    fun `a source's own id keeps a separator it carries`() {
        val id = MovieId(ONE_SOURCE, MOVIE_ID_CARRYING_A_SEPARATOR)

        assertEquals(MOVIE_ID_CARRYING_A_SEPARATOR, MovieId.of(id.qualified).sourceMovieId)
    }
}
