package com.shayan.amro.core.network.sources.tmdb.mapper

import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey

/** What TMDB reads as a union of genres. A comma in its place would read as an intersection. */
private const val GENRE_UNION_SEPARATOR = "|"

/** What separates a sort field from the direction it runs in. */
private const val SORT_SEPARATOR = "."

/**
 * The title TMDB orders on.
 *
 * The app sends no language, so it holds original titles. Ordering on the localised title would
 * order by a string the provider chose while the app holds another.
 */
private const val ORIGINAL_TITLE_FIELD = "original_title"

private const val POPULARITY_FIELD = "popularity"

private const val PRIMARY_RELEASE_DATE_FIELD = "primary_release_date"

private const val ASCENDING_TOKEN = "asc"

private const val DESCENDING_TOKEN = "desc"

/**
 * A selection in the words TMDB's discover endpoint takes.
 *
 * @property sortBy what `sort_by` carries.
 * @property withGenres what `with_genres` carries, or null when the parameter is omitted.
 */
internal data class TmdbDiscoverQuery(
    val sortBy: String,
    val withGenres: String?,
)

/**
 * Reads one selection as the query that asks TMDB for it.
 *
 * Genre ids are ordered so that the same selection always writes the same parameter, whichever
 * order the user picked the chips in.
 */
internal fun MovieSelection.toDiscoverQuery(): TmdbDiscoverQuery =
    TmdbDiscoverQuery(
        sortBy = sort.toSortBy(),
        withGenres =
            genres
                .takeIf { it.isNotEmpty() }
                ?.map(::tmdbGenreId)
                ?.sorted()
                ?.joinToString(GENRE_UNION_SEPARATOR),
    )

/** The token `sort_by` takes for one ordering. */
private fun MovieSort.toSortBy(): String = key.field + SORT_SEPARATOR + direction.token

/** The field TMDB orders on for one key. */
private val SortKey.field: String
    get() =
        when (this) {
            SortKey.POPULARITY -> POPULARITY_FIELD
            SortKey.TITLE -> ORIGINAL_TITLE_FIELD
            SortKey.RELEASE_DATE -> PRIMARY_RELEASE_DATE_FIELD
        }

/** What TMDB calls one direction. */
private val SortDirection.token: String
    get() =
        when (this) {
            SortDirection.ASCENDING -> ASCENDING_TOKEN
            SortDirection.DESCENDING -> DESCENDING_TOKEN
        }
