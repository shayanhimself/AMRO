package com.shayan.amro.feature.detail.component

import androidx.compose.ui.unit.dp

/** The poster that carries over from the row the user tapped, at the 2:3 every source serves. */
internal val POSTER_WIDTH = 92.dp
internal val POSTER_HEIGHT = 138.dp

/** The line separating the poster from the art behind it. */
internal val POSTER_OUTLINE_WIDTH = 1.dp

/** How tall the backdrop opens the screen, and the number the collapsing bar is measured against. */
internal val BACKDROP_HEIGHT = 200.dp

/** How far the poster is pulled up over the backdrop. */
private val POSTER_OVERLAP = 48.dp

/**
 * Poster overlaps the backdrop, so the header height is shorter than the sum of the two.
 */
internal val HEADER_HEIGHT = BACKDROP_HEIGHT + POSTER_HEIGHT - POSTER_OVERLAP
