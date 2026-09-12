package com.shayan.amro.core.testing.fake

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.shayan.amro.core.data.MoviesRepository
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A movies repository over a seeded page per selection, recording what was asked of it.
 *
 * A selection nothing was seeded for reads as an empty list, which is what a device holding no
 * cache for it renders.
 */
class FakeMoviesRepository : MoviesRepository {
    private val pages = MutableStateFlow<Map<MovieSelection, List<Movie>>>(emptyMap())
    private val recordedSelections = mutableListOf<MovieSelection>()

    /** The selection each [moviesPagingFlow] call asked for, in call order. */
    val requestedSelections: List<MovieSelection> get() = recordedSelections.toList()

    /**
     * Sets what one selection reads as, as a list cached before the test began.
     *
     * @param selection which narrowing and ordering the movies belong to.
     * @param movies what its pages hold, in the order they hold them.
     */
    fun seed(
        selection: MovieSelection,
        movies: List<Movie>,
    ) {
        pages.value += (selection to movies)
    }

    override fun moviesPagingFlow(selection: MovieSelection): Flow<PagingData<Movie>> {
        recordedSelections += selection
        return pages.map {
            PagingData.from(data = it[selection].orEmpty(), sourceLoadStates = SETTLED)
        }
    }
}

/** A list with nothing left to load, which is every list this fake serves. */
private val SETTLED =
    LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )
