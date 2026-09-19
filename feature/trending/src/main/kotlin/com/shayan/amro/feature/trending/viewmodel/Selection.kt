package com.shayan.amro.feature.trending.viewmodel

/**
 * The filter and sort the user has chosen.
 *
 * @property genreNames the selected genres, as the enum's own names.
 * @property sort the order the list is in.
 */
internal data class Selection(
    val genreNames: List<String> = emptyList(),
    val sort: MovieSort = MovieSort.DEFAULT,
)
