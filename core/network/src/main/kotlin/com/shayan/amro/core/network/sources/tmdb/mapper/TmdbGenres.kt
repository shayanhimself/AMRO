package com.shayan.amro.core.network.sources.tmdb.mapper

import com.shayan.amro.core.model.Genre

/**
 * TMDB's genre ids paired with the app genre each one becomes.
 *
 * The id is the key rather than the name, because an id survives a rename and a name does not.
 */
private val GENRES_BY_TMDB_ID =
    mapOf(
        28 to Genre.ACTION,
        12 to Genre.ADVENTURE,
        16 to Genre.ANIMATION,
        35 to Genre.COMEDY,
        80 to Genre.CRIME,
        99 to Genre.DOCUMENTARY,
        18 to Genre.DRAMA,
        10751 to Genre.FAMILY,
        14 to Genre.FANTASY,
        36 to Genre.HISTORY,
        27 to Genre.HORROR,
        10402 to Genre.MUSIC,
        9648 to Genre.MYSTERY,
        10749 to Genre.ROMANCE,
        878 to Genre.SCIENCE_FICTION,
        10770 to Genre.TV_MOVIE,
        53 to Genre.THRILLER,
        10752 to Genre.WAR,
        37 to Genre.WESTERN,
    )

/**
 * Every TMDB genre id the table carries.
 *
 * The live check reads it to find an entry TMDB has stopped publishing, which `tmdbGenre` cannot
 * surface because nothing ever asks it for a genre that no longer exists.
 */
internal val tmdbGenreIds: Set<Int> get() = GENRES_BY_TMDB_ID.keys

/**
 * The app genre one of TMDB's ids becomes.
 *
 * @return null for an id the table does not carry, which drops it, so every genre on a screen is
 * one the filter sheet also offers.
 */
internal fun tmdbGenre(id: Int): Genre? = GENRES_BY_TMDB_ID[id]
