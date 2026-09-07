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
 * How long a live fetch of the trending set may take, which is six or more requests paged in order
 * over whatever connection the device has.
 */
internal const val LIST_TIMEOUT_MILLIS = 60_000L

/**
 * How many swipes may pass before the list is treated as unable to reach its end. Generous: the
 * set is a hundred rows and a swipe carries a few of them.
 */
private const val MAX_SWIPES = 200

/**
 * Where a swipe starts and ends down the list, and how long the finger takes to get there.
 *
 * A default `swipeUp` is a flick: it flings, and a hundred rows go past in five of them with
 * nothing legible on screen. Half a list-height over a third of a second is a drag rather than a
 * flick, so it carries about a screenful and leaves the scrolling visible to anyone watching.
 */
private const val SWIPE_START_FRACTION = 0.8f
private const val SWIPE_END_FRACTION = 0.3f
private const val SWIPE_DURATION_MILLIS = 300L

/** The trending list, which is the only scrollable on the screen the app launches to. */
internal fun ComposeTestRule.trendingList(): SemanticsNodeInteraction = onNode(hasScrollAction())

/** How many rows the list holds.
 *  The count comes off the list itself, so the scroll cannot go out of bounds.
 */
internal fun ComposeTestRule.rowCount(): Int =
    trendingList().fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount

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

/**
 * Swipes the list to its end, the way a reader reaches it.
 * The end is where the list reports nothing further to scroll to.
 */
internal fun ComposeTestRule.swipeTrendingListToEnd() {
    repeat(MAX_SWIPES) {
        if (!canTrendingListScrollFurther()) return
        trendingList().performTouchInput {
            swipeUp(
                startY = height * SWIPE_START_FRACTION,
                endY = height * SWIPE_END_FRACTION,
                durationMillis = SWIPE_DURATION_MILLIS,
            )
        }
        waitForIdle()
    }
    error("the list still had further to go after $MAX_SWIPES swipes")
}

/** Whether the list reports anything below what it is showing. */
private fun ComposeTestRule.canTrendingListScrollFurther(): Boolean {
    val range =
        trendingList()
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
    return range.value() < range.maxValue()
}
