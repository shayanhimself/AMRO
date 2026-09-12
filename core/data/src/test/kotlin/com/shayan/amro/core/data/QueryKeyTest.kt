package com.shayan.amro.core.data

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryKeyTest {
    @Test
    fun `a selection and its reverse produce one key`() {
        val one = MovieSelection(genres = setOf(Genre.ACTION, Genre.COMEDY)).queryKey()
        val other = MovieSelection(genres = setOf(Genre.COMEDY, Genre.ACTION)).queryKey()

        assertEquals(one, other)
    }

    @Test
    fun `a key names the genres, the key and the direction`() {
        val key =
            MovieSelection(
                genres = setOf(Genre.COMEDY, Genre.ACTION),
                sort = MovieSort(SortKey.POPULARITY, SortDirection.DESCENDING),
            ).queryKey()

        assertEquals("ACTION,COMEDY:POPULARITY:DESCENDING", key)
    }

    @Test
    fun `an empty selection leaves the genre segment empty`() {
        val key = MovieSelection().queryKey()

        assertTrue(key.startsWith(":"), "the unfiltered list collides with a filtered one: $key")
    }

    @Test
    fun `every narrowing and ordering is a key of its own`() {
        val keys =
            SortKey.entries.flatMap { key ->
                SortDirection.entries.flatMap { direction ->
                    listOf(emptySet(), setOf(Genre.COMEDY), setOf(Genre.COMEDY, Genre.HORROR))
                        .map { MovieSelection(it, MovieSort(key, direction)).queryKey() }
                }
            }

        assertEquals(keys.distinct(), keys)
    }
}
