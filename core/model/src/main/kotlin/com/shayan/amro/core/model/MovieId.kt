package com.shayan.amro.core.model

/** Identifies the provider a record came from.
 * Class instead of a String to avoid accidental mixing of source and value.
 */
@JvmInline
value class SourceId(
    val value: String,
)

/**
 * Identifies one movie.
 *
 * The id space belongs to whoever issued it, so the source travels with it and two providers'
 * integer `1` are two different movies.
 *
 * @property source the provider that issued [value].
 * @property value the id as that provider wrote it.
 */
data class MovieId(
    val source: SourceId,
    val value: String,
)
