package com.shayan.amro.core.ui.component.placeholder

import kotlin.random.Random

/**
 * A width inside [range].
 *
 * @return the share of its container a placeholder line runs to.
 */
fun Random.nextWidth(range: ClosedFloatingPointRange<Float>): Float =
    nextDouble(
        from = range.start.toDouble(),
        until = range.endInclusive.toDouble(),
    ).toFloat()
