package com.shayan.amro.feature.trending.viewmodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** What a list of movies is ordered by. */
internal enum class SortKey {
    POPULARITY,
    TITLE,
    RELEASE_DATE,
}

/** Which way an ordering runs. */
internal enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

/**
 * How a list of movies is ordered.
 *
 * The key and the direction are one type because they always travel together: one thing is saved,
 * restored and passed.
 *
 * It is `Parcelable` because a `SavedStateHandle` writes into a `Bundle`, which takes only what the
 * platform can write. Holding the pair as one savable value is what keeps the ordering in one place
 * rather than two keys rebuilt into a type on every emission.
 */
@Parcelize
internal data class MovieSort(
    val key: SortKey,
    val direction: SortDirection,
) : Parcelable {
    companion object {
        /** The order the trending list opens on. */
        val DEFAULT = MovieSort(key = SortKey.POPULARITY, direction = SortDirection.DESCENDING)
    }
}
