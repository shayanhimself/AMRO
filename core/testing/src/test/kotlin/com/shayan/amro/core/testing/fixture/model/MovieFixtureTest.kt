package com.shayan.amro.core.testing.fixture.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** Enough entries for the genres to come round more than once. */
private const val SET_SIZE = 12

class MovieFixtureTest {
    @Test
    fun `a ranked list holds as many movies as it was asked for`() {
        assertEquals(SET_SIZE, MovieFixture.rankedMovies(SET_SIZE).size)
    }

    @Test
    fun `a ranked list is already ordered, so returning it is the default ordering`() {
        val popularity = MovieFixture.rankedMovies(SET_SIZE).map { it.popularity }

        assertEquals(popularity.sortedDescending(), popularity)
    }

    @Test
    fun `no two movies in a ranked list share an id, a title or a release date`() {
        val movies = MovieFixture.rankedMovies(SET_SIZE)

        assertEquals(SET_SIZE, movies.distinctBy { it.id }.size, "ids")
        assertEquals(SET_SIZE, movies.distinctBy { it.title }.size, "titles")
        assertEquals(SET_SIZE, movies.distinctBy { it.releaseDate }.size, "release dates")
    }

    @Test
    fun `the genres cycle, so a filter for one selects an even share of the set`() {
        val perGenre =
            MovieFixture
                .rankedMovies(SET_SIZE)
                .flatMap { it.genres }
                .groupingBy { it }
                .eachCount()

        assertEquals(setOf(SET_SIZE / perGenre.size), perGenre.values.toSet())
    }
}
