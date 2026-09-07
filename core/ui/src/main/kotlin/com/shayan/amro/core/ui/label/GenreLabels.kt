package com.shayan.amro.core.ui.label

import androidx.annotation.StringRes
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.ui.R

/**
 * What a genre is called.
 */
@get:StringRes
val Genre.labelRes: Int
    get() =
        when (this) {
            Genre.ACTION -> R.string.core_ui_genre_action
            Genre.ADVENTURE -> R.string.core_ui_genre_adventure
            Genre.ANIMATION -> R.string.core_ui_genre_animation
            Genre.COMEDY -> R.string.core_ui_genre_comedy
            Genre.CRIME -> R.string.core_ui_genre_crime
            Genre.DOCUMENTARY -> R.string.core_ui_genre_documentary
            Genre.DRAMA -> R.string.core_ui_genre_drama
            Genre.FAMILY -> R.string.core_ui_genre_family
            Genre.FANTASY -> R.string.core_ui_genre_fantasy
            Genre.HISTORY -> R.string.core_ui_genre_history
            Genre.HORROR -> R.string.core_ui_genre_horror
            Genre.MUSIC -> R.string.core_ui_genre_music
            Genre.MYSTERY -> R.string.core_ui_genre_mystery
            Genre.ROMANCE -> R.string.core_ui_genre_romance
            Genre.SCIENCE_FICTION -> R.string.core_ui_genre_science_fiction
            Genre.TV_MOVIE -> R.string.core_ui_genre_tv_movie
            Genre.THRILLER -> R.string.core_ui_genre_thriller
            Genre.WAR -> R.string.core_ui_genre_war
            Genre.WESTERN -> R.string.core_ui_genre_western
        }
