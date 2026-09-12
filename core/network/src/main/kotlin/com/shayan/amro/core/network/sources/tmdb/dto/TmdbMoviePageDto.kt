package com.shayan.amro.core.network.sources.tmdb.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * One page of TMDB's discover response.
 *
 * `results` has no default: a response without it cannot become movies at all, so it fails to
 * parse and surfaces as a server failure.
 *
 * The page number and the totals TMDB sends beside it have no consumer. `total_pages` counts what
 * exists rather than what is served, and the walk ends on the deepest page the endpoint answers.
 */
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class TmdbMoviePageDto(
    val results: List<TmdbMovieDto>,
)
