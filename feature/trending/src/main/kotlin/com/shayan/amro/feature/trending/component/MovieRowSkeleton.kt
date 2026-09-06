package com.shayan.amro.feature.trending.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import kotlin.random.Random

/** How long one full breath of a placeholder takes. */
private const val PULSE_DURATION_MILLIS = 1_000

/**
 * How far down a breath carries a placeholder. The alpha scales a block that already sits at a
 * fraction of the foreground, so the two multiply: a floor near the top of the range moves the
 * drawn colour by a step too small to see.
 */
private const val PULSE_MIN_ALPHA = 0.40f
private const val PULSE_MAX_ALPHA = 1f

/**
 * How much of the foreground a placeholder block carries over the surface behind it, and how much
 * the lesser of the two carries. A light scheme takes more of it to read at the same strength.
 */
private const val PLACEHOLDER_SHARE_DARK = 0.20f
private const val PLACEHOLDER_SHARE_LIGHT = 0.32f
private const val MUTED_PLACEHOLDER_SHARE_DARK = 0.13f
private const val MUTED_PLACEHOLDER_SHARE_LIGHT = 0.22f

/** Below this, a surface is a dark scheme's. */
private const val DARK_SURFACE_LUMINANCE = 0.5f

/** How wide the line standing in for a title runs, as a share of the space beside the poster. */
private val TITLE_LINE_WIDTHS = 0.60f..0.90f

/**
 * How wide the line standing in for the genres runs.
 *
 * The range sits wholly below the title's, so a row can never draw its genres longer than the
 * title above them and the skeleton keeps the shape of the rows it stands in for.
 */
private val GENRE_LINE_WIDTHS = 0.30f..0.55f

/** How many rows a breath is offset across before the offset repeats. */
private const val PULSE_PHASES = 3

/** How far one row's breath is offset from the row before it. */
private const val PULSE_STAGGER_MILLIS = 120

/** How many rows the preview draws, which is enough to show the widths and the offsets varying. */
private const val PREVIEW_ROW_COUNT = 3

/**
 * A row shaped like [MovieRow], with nothing in it yet.
 *
 * @param index which row of the skeleton this is, which is what varies the placeholder widths and
 * offsets one row's breath from the next.
 */
@Composable
internal fun MovieRowSkeleton(
    index: Int,
    modifier: Modifier = Modifier,
) {
    // Widths are drawn from the row's own index rather than from chance, so a row keeps its shape
    // across a recomposition and a golden captures the same skeleton every time.
    val (titleWidth, genreWidth) =
        remember(index) {
            val widths = Random(index)
            widths.nextWidth(TITLE_LINE_WIDTHS) to widths.nextWidth(GENRE_LINE_WIDTHS)
        }
    Row(
        modifier = modifier.fillMaxWidth().alpha(pulseAlpha(index % PULSE_PHASES)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(
            modifier = Modifier.width(POSTER_WIDTH).height(POSTER_HEIGHT),
            shape = RoundedCornerShape(RadiusPrimitives.radius2),
            color = placeholderColor(),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            PlaceholderBlock(
                modifier =
                    Modifier
                        .fillMaxWidth(titleWidth)
                        .height(Spacing.s4),
                shape = RoundedCornerShape(RadiusPrimitives.radius1),
                color = placeholderColor(),
            )
            PlaceholderBlock(
                modifier =
                    Modifier
                        .fillMaxWidth(genreWidth)
                        .height(Spacing.s3),
                shape = RoundedCornerShape(RadiusPrimitives.radius1),
                color = mutedPlaceholderColor(),
            )
        }
    }
}

/**
 * One shape a skeleton row stands a real element in for.
 *
 * @param color how strongly it reads against the surface behind it.
 */
@Composable
private fun PlaceholderBlock(
    modifier: Modifier,
    shape: Shape,
    color: Color,
) {
    Box(modifier = modifier.clip(shape).background(color))
}

/**
 * A width inside [range].
 *
 * @return the share of the row the line runs to.
 */
private fun Random.nextWidth(range: ClosedFloatingPointRange<Float>): Float =
    nextDouble(range.start.toDouble(), range.endInclusive.toDouble()).toFloat()

/** The colour a placeholder block reads in. */
@Composable
private fun placeholderColor(): Color =
    mixedIntoSurface(PLACEHOLDER_SHARE_DARK, PLACEHOLDER_SHARE_LIGHT)

/** The lesser of the two, which the line standing in for the genres carries. */
@Composable
private fun mutedPlaceholderColor(): Color =
    mixedIntoSurface(MUTED_PLACEHOLDER_SHARE_DARK, MUTED_PLACEHOLDER_SHARE_LIGHT)

/**
 * Mixes the foreground into the surface behind it, by the share the active scheme asks for.
 *
 * The two shares differ because the same amount of foreground reads weaker on a light surface than
 * on a dark one, and which scheme is in force is read from the surface rather than passed down,
 * so nothing above a placeholder has to carry it.
 *
 * @param darkShare how much foreground a dark scheme takes.
 * @param lightShare how much a light one takes.
 */
@Composable
private fun mixedIntoSurface(
    darkShare: Float,
    lightShare: Float,
): Color {
    val colors = MaterialTheme.colorScheme
    val share = if (colors.surface.luminance() < DARK_SURFACE_LUMINANCE) darkShare else lightShare
    return colors.onSurface.copy(alpha = share).compositeOver(colors.surface)
}

/**
 * The breathing opacity of a placeholder.
 *
 * A preview and the golden that captures it render one frame, so the animation is not started
 * there: a golden of a running animation fails on its own schedule. They draw the breath at its
 * full extent, which is the placeholder at the strength the shape was picked to read at.
 *
 * @param step which of the staggered starts this row takes.
 * @return the alpha to draw the row at.
 */
@Composable
private fun pulseAlpha(step: Int): Float {
    if (LocalInspectionMode.current) return PULSE_MAX_ALPHA
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = PULSE_MIN_ALPHA,
        targetValue = PULSE_MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = PULSE_DURATION_MILLIS),
                repeatMode = RepeatMode.Reverse,
                // The offset starts the row partway into the first breath. A delay on the
                // animation itself would instead be paid on every repetition, which holds each row
                // still for longer than the one above it and gives the rows different periods.
                initialStartOffset = StartOffset(offsetMillis = step * PULSE_STAGGER_MILLIS),
            ),
        label = "skeleton-alpha",
    )
    return alpha
}

@Preview
@Composable
private fun MovieRowSkeletonPreview() {
    AmroTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.s4),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                repeat(PREVIEW_ROW_COUNT) { index -> MovieRowSkeleton(index = index) }
            }
        }
    }
}
