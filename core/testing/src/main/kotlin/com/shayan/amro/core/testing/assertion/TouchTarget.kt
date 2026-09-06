package com.shayan.amro.core.testing.assertion

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

/** Smallest size Material accepts a tappable element at, on either axis. */
private val TOUCH_TARGET_MIN = 48.dp

/** A touch target measured in device pixels lands a rounding away from the dp it was given. */
private val TOUCH_TARGET_TOLERANCE = 0.5.dp

/**
 * Asserts the node's touch target is at least the minimum tappable size on both axes.
 *
 * @param density the density the node was measured at, which is what turns its pixels back into dp.
 */
fun SemanticsNodeInteraction.assertTouchTargetMeetsMinimum(density: Density) {
    val touchBounds = fetchSemanticsNode().touchBoundsInRoot
    with(density) {
        val height = touchBounds.height.toDp()
        val width = touchBounds.width.toDp()
        assertTrue(
            "touch height $height is below $TOUCH_TARGET_MIN",
            height + TOUCH_TARGET_TOLERANCE >= TOUCH_TARGET_MIN,
        )
        assertTrue(
            "touch width $width is below $TOUCH_TARGET_MIN",
            width + TOUCH_TARGET_TOLERANCE >= TOUCH_TARGET_MIN,
        )
    }
}
