package com.shayan.amro.flow

import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick

private const val APPEARANCE_TIMEOUT_MILLIS = 10_000L
private const val SETTLE_TIMEOUT_MILLIS = 5_000L
private const val STILL_MILLIS = 300L

/**
 * Blocks until [text] is on screen, and fails the test once the timeout passes without it.
 *
 * A flow waits on work no idling resource covers: a response crosses a real socket and the
 * database writes on its own dispatcher, so a composition that has gone idle says nothing about
 * whether either has landed.
 */
internal fun ComposeTestRule.awaitText(
    text: String,
    timeoutMillis: Long = APPEARANCE_TIMEOUT_MILLIS,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Blocks until a node described as [description] is on screen.
 *
 * What a control reports it does is the only evidence from outside the app for a state its text
 * does not name, such as a sort direction or a back affordance that is present on one pane count
 * and absent on the other.
 */
internal fun ComposeTestRule.awaitContentDescription(
    description: String,
    timeoutMillis: Long = APPEARANCE_TIMEOUT_MILLIS,
) {
    waitUntil(timeoutMillis) {
        onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Clicks the node [matcher] finds, once it has stayed still in the same place for [STILL_MILLIS].
 *
 * A sheet opening and a list settling both move the layout under the test for several frames.
 * Nothing reports that motion, since the composition is idle throughout, so a tap sent during it
 * is injected where the target was when it was aimed and lands beside it.
 *
 * Waiting for the node to stop moving ends when the animation does rather than after a guessed
 * number of milliseconds, and both the position within the window and the window's own position
 * count, because a window can either resize the layout or slide whole.
 */
internal fun ComposeTestRule.clickWhenStill(matcher: SemanticsMatcher) {
    val node = onNode(matcher)
    var previous: Pair<Any, Any>? = null
    var unchangedSince = 0L
    waitUntil(SETTLE_TIMEOUT_MILLIS) {
        val semantics = node.fetchSemanticsNode()
        val place = semantics.boundsInRoot to semantics.positionOnScreen
        val now = SystemClock.uptimeMillis()
        if (place != previous) {
            previous = place
            unchangedSince = now
            false
        } else {
            now - unchangedSince >= STILL_MILLIS
        }
    }
    node.performClick()
}
