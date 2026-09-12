package com.shayan.amro.core.data

import androidx.paging.PagingData
import com.shayan.amro.core.model.Movie
import com.shayan.amro.core.model.MovieSelection
import kotlinx.coroutines.flow.Flow

/**
 * Provides the movies one selection describes, a page at a time.
 */
interface MoviesRepository {
    /**
     * The movies a selection narrows and orders.
     *
     * The pages are served from the database and filled from a source, so a selection this device
     * has already run renders with no network at all.
     *
     * @param selection which movies to hold, and in what order.
     * @return the pages, with what a fetch did carried in their load states.
     */
    fun moviesPagingFlow(selection: MovieSelection): Flow<PagingData<Movie>>
}
