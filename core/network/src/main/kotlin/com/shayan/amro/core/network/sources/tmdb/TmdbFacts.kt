package com.shayan.amro.core.network.sources.tmdb

/**
 * How many movies TMDB serves per page.
 *
 * TBDM ignores every page size parameter, so this is a fact the source works with rather than a
 * default it can override. Nothing outside this module knows it.
 */
internal const val TMDB_PAGE_SIZE = 20

/**
 * How many pages one trending walk may ask for.
 *
 * TMDB re-ranks between requests, so a movie can appear on two sequential pages while another is
 * missed. Reaching a count therefore means paging until enough distinct movies are held, and this
 * is what stops a pathological response from looping.
 */
internal const val TMDB_PAGE_CAP = 10
