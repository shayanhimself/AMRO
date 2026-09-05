package com.shayan.amro.feature.trending.viewmodel

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Movie

/**
 * Narrows a list of movies to the ones a genre selection keeps.
 *
 * @param genres to filter by. If empty, no filtering is applied.
 * @return the movies carrying any selected genre, in the order they were given.
 */
internal fun List<Movie>.applyFilter(genres: Set<Genre>): List<Movie> {
    if (genres.isEmpty()) return this
    return filter { movie -> movie.genres.any { it in genres } }
}
