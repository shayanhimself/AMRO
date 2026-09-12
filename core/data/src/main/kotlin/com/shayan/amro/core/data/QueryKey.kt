package com.shayan.amro.core.data

import com.shayan.amro.core.model.MovieSelection

/** What separates the genres, the key and the direction inside one query key. */
private const val SEGMENT_SEPARATOR = ":"

/** What separates two genre names inside the first segment. */
private const val GENRE_SEPARATOR = ","

/**
 * The key naming this narrowing and ordering, as `ACTION,COMEDY:POPULARITY:DESCENDING`.
 *
 * It is built from the selection alone, so the same narrowing and ordering is one cache whichever
 * source answered it. Genre names are sorted, so a selection made in either order is one key, and
 * they are names rather than ordinals for the reason the stored genres already use them.
 */
internal fun MovieSelection.queryKey(): String =
    listOf(
        genres.map { it.name }.sorted().joinToString(GENRE_SEPARATOR),
        sort.key.name,
        sort.direction.name,
    ).joinToString(SEGMENT_SEPARATOR)
