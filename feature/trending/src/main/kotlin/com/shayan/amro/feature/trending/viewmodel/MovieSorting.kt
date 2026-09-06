package com.shayan.amro.feature.trending.viewmodel

import com.shayan.amro.core.model.Movie

/**
 * Sorts a list of movies.
 *
 * @param sort which key to order on, and which direction.
 * @return the movies in that order. A tie gets ordered by id.
 */
internal fun List<Movie>.applySort(sort: MovieSort): List<Movie> = sortedWith(comparatorFor(sort))

/**
 * Compares movies by the given sort key and direction, then by their id.
 */
private fun comparatorFor(sort: MovieSort): Comparator<Movie> {
    val byKey: Comparator<Movie> =
        when (sort.key) {
            SortKey.POPULARITY -> {
                compareBy(
                    sort.direction.applyTo(naturalOrder()),
                ) { it.popularity }
            }

            SortKey.TITLE -> {
                compareBy(
                    sort.direction.applyTo(String.CASE_INSENSITIVE_ORDER),
                ) { it.title }
            }

            SortKey.RELEASE_DATE -> {
                compareBy(
                    nullsLast(sort.direction.applyTo(naturalOrder())),
                ) { it.releaseDate }
            }
        }
    return byKey.thenBy { it.id }
}

/**
 * Reads a comparator in this direction.
 *
 * @param comparator the ascending ordering.
 * @return that comparator, or its reverse.
 */
private fun <T> SortDirection.applyTo(comparator: Comparator<T>): Comparator<T> =
    when (this) {
        SortDirection.ASCENDING -> comparator
        SortDirection.DESCENDING -> comparator.reversed()
    }
