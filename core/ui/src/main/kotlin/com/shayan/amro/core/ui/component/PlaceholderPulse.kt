package com.shayan.amro.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode

/** How long one full breath of a placeholder takes. */
private const val PULSE_DURATION_MILLIS = 1_000

/**
 * How far down a breath carries a placeholder.
 */
private const val PULSE_MIN_ALPHA = 0.40f
private const val PULSE_MAX_ALPHA = 1f

/** The alpha of a placeholder for previews. It holds still so a golden is stable. */
private object StillPulse : State<Float> {
    override val value: Float = PULSE_MAX_ALPHA
}

/**
 * The breathing opacity every screen's placeholders are drawn at.
 *
 * It is a [State] rather than a number so the read lands in [pulsingAlpha], in the draw phase.
 * Returning the number would record the read against the caller, and every frame of the breath
 * would recompose it.
 *
 * @param startOffsetMillis how far into the first breath this placeholder starts, to give two
 * placeholders different periods.
 */
@Composable
fun rememberPlaceholderPulseAlpha(startOffsetMillis: Int = 0): State<Float> {
    if (LocalInspectionMode.current) return StillPulse
    val transition = rememberInfiniteTransition(label = "placeholder-pulse")
    return transition.animateFloat(
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
}

/**
 * Draws the content at [alpha], read on each frame without recomposing.
 *
 * @param alpha the breath from [rememberPlaceholderPulseAlpha].
 */
fun Modifier.pulsingAlpha(alpha: State<Float>): Modifier =
    graphicsLayer { this.alpha = alpha.value }
