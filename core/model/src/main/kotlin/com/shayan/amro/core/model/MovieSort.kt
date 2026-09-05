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
        /** The order the trending list opens on. */
        val DEFAULT = MovieSort(key = SortKey.POPULARITY, direction = SortDirection.DESCENDING)
    }
}
