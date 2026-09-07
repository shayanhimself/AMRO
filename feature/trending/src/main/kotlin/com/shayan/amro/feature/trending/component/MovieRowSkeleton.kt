package com.shayan.amro.feature.trending.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.component.PlaceholderBlock
import com.shayan.amro.core.ui.component.placeholderPulseAlpha
import com.shayan.amro.core.ui.designsystem.theme.AmroExtendedTheme
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import kotlin.random.Random

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
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(rowPulseAlpha(index)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(
            modifier =
                Modifier
                    .width(POSTER_WIDTH)
                    .height(POSTER_HEIGHT),
            shape = RoundedCornerShape(RadiusPrimitives.radius2),
            color = AmroExtendedTheme.colors.placeholder,
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
                color = AmroExtendedTheme.colors.placeholder,
            )
            PlaceholderBlock(
                modifier =
                    Modifier
                        .fillMaxWidth(genreWidth)
                        .height(Spacing.s3),
                color = AmroExtendedTheme.colors.placeholderMuted,
            )
        }
    }
}

/**
 * A width inside [range].
 *
 * @return the share of the row the line runs to.
 */
private fun Random.nextWidth(range: ClosedFloatingPointRange<Float>): Float =
    nextDouble(range.start.toDouble(), range.endInclusive.toDouble()).toFloat()

/**
 * The breathing opacity this row is drawn at, offset from the row before it so the list does not
 * breathe as one block.
 *
 * @param index which row this is.
 * @return the alpha to draw the row at.
 */
@Composable
private fun rowPulseAlpha(index: Int): Float =
    placeholderPulseAlpha(
        startOffsetMillis = (index % PULSE_PHASES) * PULSE_STAGGER_MILLIS,
    )

@Preview
@Composable
private fun MovieRowSkeletonPreview() {
    AmroTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.s4),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                MovieRowSkeleton(index = 0)
            }
        }
    }
}
