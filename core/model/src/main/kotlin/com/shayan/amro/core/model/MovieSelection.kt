package com.shayan.amro.core.model

/** What a list of movies is ordered by. */
enum class SortKey {
    POPULARITY,
    TITLE,
    RELEASE_DATE,
}

/** Which way an ordering runs. */
enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

/**
 * How a list of movies is ordered.
 *
 * The key and the direction are one type because they always travel together: one thing is saved,
 * restored and passed.
 */
data class MovieSort(
    val key: SortKey,
    val direction: SortDirection,
) {
    companion object {
        /** The order the list opens on. */
        val DEFAULT = MovieSort(key = SortKey.POPULARITY, direction = SortDirection.DESCENDING)
    }
}

/**
 * Which movies a list holds, and in what order.
 *
 * A source narrows and orders its own catalogue from this, so it reaches the network layer rather
 * than stopping at the screen that holds it.
 *
 * @property genres the movies to keep, matched as a union. An empty set keeps everything.
 * @property sort the order the kept movies arrive in.
 */
data class MovieSelection(
    val genres: Set<Genre> = emptySet(),
    val sort: MovieSort = MovieSort.DEFAULT,
)
