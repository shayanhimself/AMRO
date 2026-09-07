package com.shayan.amro.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode

/** How long one full breath of a placeholder takes. */
private const val PULSE_DURATION_MILLIS = 1_000

/**
 * How far down a breath carries a placeholder.
 */
private const val PULSE_MIN_ALPHA = 0.40f
private const val PULSE_MAX_ALPHA = 1f

/**
 * The breathing opacity every screen's placeholders are drawn at.
 *
 * @param startOffsetMillis how far into the first breath this placeholder starts, to give two
 * placeholders different periods.
 */
@Composable
fun placeholderPulseAlpha(startOffsetMillis: Int = 0): Float {
    if (LocalInspectionMode.current) return PULSE_MAX_ALPHA
    val transition = rememberInfiniteTransition(label = "placeholder-pulse")
    val alpha by transition.animateFloat(
        initialValue = PULSE_MIN_ALPHA,
        targetValue = PULSE_MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = PULSE_DURATION_MILLIS),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(offsetMillis = startOffsetMillis),
            ),
        label = "placeholder-pulse-alpha",
    )
    return alpha
}
