package com.shayan.amro.core.network.sources.tmdb

/**
 * How many movies TMDB serves per page.
 *
 * TMDB ignores every page size parameter, so this is a fact the source works with rather than a
 * default it can override. Nothing outside this module knows it.
 */
internal const val TMDB_PAGE_SIZE = 20

/**
 * The deepest page TMDB answers.
 *
 * `page=501` answers 400, so a walk ends here rather than on the `total_pages` the response
 * reports, which counts what exists rather than what is served.
 */
internal const val TMDB_LAST_PAGE = 500
