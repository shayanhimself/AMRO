package com.shayan.amro.core.testing.fake

import androidx.paging.testing.asSnapshot
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.testing.fixture.model.MovieFixture.MOVIE
import com.shayan.amro.core.testing.fixture.model.MovieFixture.OTHER_MOVIE
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val SELECTION = MovieSelection(genres = setOf(Genre.COMEDY))

private val OTHER_SELECTION = MovieSelection(genres = setOf(Genre.HORROR))

class FakeMoviesRepositoryTest {
    private val repository = FakeMoviesRepository()

    @Test
    fun `a seeded selection reads as the movies it was seeded with`() =
        runTest {
            repository.seed(SELECTION, listOf(MOVIE, OTHER_MOVIE))

            assertEquals(
                listOf(MOVIE, OTHER_MOVIE),
                repository.moviesPagingFlow(SELECTION).asSnapshot(),
            )
        }

    @Test
    fun `a selection nothing was seeded for reads as an empty list`() =
        runTest {
            repository.seed(SELECTION, listOf(MOVIE))

            assertEquals(emptyList(), repository.moviesPagingFlow(OTHER_SELECTION).asSnapshot())
        }

    @Test
    fun `the selection each read asked for is recorded`() =
        runTest {
            repository.seed(SELECTION, listOf(MOVIE))
            repository.seed(OTHER_SELECTION, listOf(OTHER_MOVIE))

            repository.moviesPagingFlow(SELECTION).asSnapshot()
            repository.moviesPagingFlow(OTHER_SELECTION).asSnapshot()

            assertEquals(listOf(SELECTION, OTHER_SELECTION), repository.requestedSelections)
        }
}
