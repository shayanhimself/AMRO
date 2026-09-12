package com.shayan.amro.e2e.screens

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

/**
 * How long a live read of the first page may take, which is a few requests paged in order over
 * whatever connection the device has.
 */
internal const val LIST_TIMEOUT_MILLIS = 60_000L

/**
 * How many swipes carry a reader past the rows a first read holds, so the list has to fetch
 * further ones to answer them.
 */
private const val SWIPES_PAST_THE_FIRST_READ = 30

/**
 * Where a swipe starts and ends down the list, and how long the finger takes to get there.
 *
 * A default `swipeUp` is a flick: it flings, and a page goes past in a few of them with
 * nothing legible on screen. Half a list-height over a third of a second is a drag rather than a
 * flick, so it carries about a screenful and leaves the scrolling visible to anyone watching.
 */
private const val SWIPE_START_FRACTION = 0.8f
private const val SWIPE_END_FRACTION = 0.3f
private const val SWIPE_DURATION_MILLIS = 300L

/** The movie list, which is the only scrollable on the screen the app launches to. */
internal fun ComposeTestRule.movieList(): SemanticsNodeInteraction = onNode(hasScrollAction())

/** How many rows the list holds.
 *  The count comes off the list itself, so the scroll cannot go out of bounds.
 */
internal fun ComposeTestRule.rowCount(): Int =
    movieList().fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount

/**
 * Blocks until the live fetch has put a titled row at the top of the list.
 *
 * @return the title the top row states.
 */
internal fun ComposeTestRule.awaitFirstRowTitle(timeoutMillis: Long = LIST_TIMEOUT_MILLIS): String {
    waitUntil(timeoutMillis) { firstRowTitle() != null }
    return checkNotNull(firstRowTitle())
}

/**
 * The title the top row states, or null while no row is stating one.
 *
 * Null covers both of the states a first load passes through, since neither the placeholders nor
 * an empty screen carries text where a row's title sits.
 */
internal fun ComposeTestRule.firstRowTitle(): String? = rowTitle { it.firstOrNull() }

/** The title the last of the rows the list currently holds composed states. */
internal fun ComposeTestRule.lastRowTitle(): String? = rowTitle { it.lastOrNull() }

/**
 * The title of one row, read the way a screen reader reaches it.
 *
 * @param pick which of the rows the list holds composed to read.
 */
private fun ComposeTestRule.rowTitle(pick: (List<SemanticsNode>) -> SemanticsNode?): String? =
    onAllNodes(hasScrollAction())
        .fetchSemanticsNodes()
        .firstOrNull()
        ?.children
        ?.let(pick)
        ?.config
        ?.getOrNull(SemanticsProperties.Text)
        ?.firstOrNull()
        ?.text

/** Swipes the list past the rows a first read holds, the way a reader reaches further ones. */
internal fun ComposeTestRule.swipeMovieListPastTheFirstRead() {
    repeat(SWIPES_PAST_THE_FIRST_READ) {
        movieList().performTouchInput {
            swipeUp(
                startY = height * SWIPE_START_FRACTION,
                endY = height * SWIPE_END_FRACTION,
                durationMillis = SWIPE_DURATION_MILLIS,
            )
        }
        waitForIdle()
    }
}
