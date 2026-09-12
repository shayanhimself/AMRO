package com.shayan.amro.core.model

/**
 * Where a source resumes a list it has already served part of.
 *
 * The value is the source's own, and nothing outside the source that wrote it reads inside one: a
 * provider paging by number writes a number, one paging by keyset writes its token.
 *
 * @property value what the source that issued it needs to serve the next movies.
 */
@JvmInline
value class Cursor(
    val value: String,
)

/**
 * One stretch of a list of movies, and where the next one starts.
 *
 * @property movies the movies, in the order the source ranks them.
 * @property next where to resume, or null when the source has served everything it will.
 */
data class MoviePage(
    val movies: List<Movie>,
    val next: Cursor?,
)
