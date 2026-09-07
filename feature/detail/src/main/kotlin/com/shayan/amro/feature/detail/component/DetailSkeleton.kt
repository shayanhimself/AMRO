package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.component.PlaceholderBlock
import com.shayan.amro.core.ui.component.PosterTile
import com.shayan.amro.core.ui.component.placeholderPulseAlpha
import com.shayan.amro.core.ui.designsystem.theme.AmroExtendedTheme
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.detail.ui.MovieDetailPreviewData
import com.shayan.amro.feature.detail.viewmodel.MovieDetailContent
import com.shayan.amro.core.ui.R as CoreUiR

/** The block standing in for the rating beside the poster, and the share of the row it runs to. */
private val BADGE_LINE_HEIGHT = Spacing.s6
private const val BADGE_LINE_WIDTH_FRACTION = 0.56f

/** The line the tagline will become, and the share of the width it runs to. */
private val TAGLINE_LINE_HEIGHT = Spacing.s4
private const val TAGLINE_LINE_WIDTH_FRACTION = 0.52f

/** Each line the overview will become, as the share of the width it runs to. */
private val OVERVIEW_LINE_HEIGHT = Spacing.s4
private val OVERVIEW_LINE_WIDTHS =
    listOf(0.96f, 0.92f, 0.97f, 0.88f, 0.94f, 0.90f, 0.96f, 0.54f)

/** Each cell the grid will become, as the share of its column the label and the value run to. */
private val FACT_LABEL_HEIGHT = Spacing.s3
private val FACT_VALUE_HEIGHT = Spacing.s4

/** Pairs of the label widths and value widths, in the order they are drawn.
 * Percentages of their parent column width.
 */
private val FACT_WIDTHS =
    listOf(
        0.44f to 0.62f,
        0.58f to 0.80f,
        0.38f to 0.72f,
        0.48f to 0.66f,
    )

/** How many cells a row of the grid holds, which is the grid the record fills. */
private const val FACTS_COLUMN_COUNT = 2

/**
 * A first load, drawn as the screen it is about to become.
 * Only the poster, the title and the genres are real, and the rest is a dozen placeholder blocks.
 *
 * @param title the movie title.
 * @param content what the summary already knows.
 * @param bottomPadding what the navigation bar takes off the bottom of the window.
 */
@Composable
internal fun DetailSkeleton(
    title: String,
    content: MovieDetailContent.Skeleton,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val pulse = placeholderPulseAlpha()
    // The header goes edge to edge, the rest have is inset.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
    ) {
        SkeletonHeader(title = title, posterUrl = content.posterUrl, pulse = pulse)

        // Insets the sections under the header, and spaces them.
        Column(
            modifier = Modifier.padding(horizontal = Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            // The title and its tagline line sit closer than two sections do.
            Column(
                modifier = Modifier.padding(top = Spacing.s5),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // The tagline's placeholder.
                PlaceholderBlock(
                    modifier =
                        Modifier
                            .fillMaxWidth(TAGLINE_LINE_WIDTH_FRACTION)
                            .height(TAGLINE_LINE_HEIGHT)
                            .alpha(pulse),
                    color = AmroExtendedTheme.colors.placeholder,
                )
            }
            if (content.genreLabels.isNotEmpty()) {
                GenreChips(
                    genreLabels = content.genreLabels,
                    description = content.genreDescription,
                )
            }
            AwaitedBody(pulse = pulse)
        }
    }
}

/**
 * The backdrop, the poster over it and the rating's placeholder.
 *
 * @param pulse the opacity the placeholders are drawn at.
 */
@Composable
private fun SkeletonHeader(
    title: String,
    posterUrl: String?,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    // The poster hangs off the bottom of the backdrop, so the two are stacked rather than in a
    // column, over the box the loaded header uses.
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT),
    ) {
        // The block the art will fill, ending in the same fade the art does.
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(BACKDROP_HEIGHT)
                    .alpha(pulse)
                    .background(AmroExtendedTheme.colors.placeholderMuted),
        ) {
            BackdropFade()
        }
        // The poster and the rating's slot, sitting on the bottom edge so the poster overlaps.
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
            verticalAlignment = Alignment.Bottom,
        ) {
            PosterTile(
                posterUrl = posterUrl,
                title = title,
                modifier =
                    Modifier
                        .width(POSTER_WIDTH)
                        .height(POSTER_HEIGHT)
                        .border(
                            width = POSTER_OUTLINE_WIDTH,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(RadiusPrimitives.radius2),
                        ),
            )
            PlaceholderBlock(
                modifier =
                    Modifier
                        .padding(bottom = Spacing.s2)
                        .fillMaxWidth(BADGE_LINE_WIDTH_FRACTION)
                        .height(BADGE_LINE_HEIGHT)
                        .alpha(pulse),
                color = AmroExtendedTheme.colors.placeholder,
            )
        }
    }
}

/**
 * The movie overview, facts placeholders.
 *
 * @param pulse the opacity the placeholders are drawn at.
 */
@Composable
private fun AwaitedBody(
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val loading = stringResource(CoreUiR.string.core_ui_loading)
    // One node for the whole region, so the wait is announced once and not per block.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(pulse)
                .semantics(mergeDescendants = true) { contentDescription = loading },
    ) {
        // Overview placeholder lines.
        Column(
            modifier = Modifier.padding(top = Spacing.s1),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            OVERVIEW_LINE_WIDTHS.forEach { width ->
                PlaceholderBlock(
                    modifier =
                        Modifier
                            .fillMaxWidth(width)
                            .height(OVERVIEW_LINE_HEIGHT),
                    color = AmroExtendedTheme.colors.placeholder,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = Spacing.s6),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        // The facts placeholder grid, two cells each row.
        Column(
            modifier = Modifier.padding(top = Spacing.s5),
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
        ) {
            FACT_WIDTHS.chunked(FACTS_COLUMN_COUNT).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                    row.forEach { (labelWidth, valueWidth) ->
                        FactPlaceholder(
                            labelWidth = labelWidth,
                            valueWidth = valueWidth,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One cell of the fact grid.
 *
 * @param labelWidth the share of the column the label runs to.
 * @param valueWidth the share of the column the value runs to.
 */
@Composable
private fun FactPlaceholder(
    labelWidth: Float,
    valueWidth: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        PlaceholderBlock(
            modifier =
                Modifier
                    .fillMaxWidth(labelWidth)
                    .height(FACT_LABEL_HEIGHT),
            color = AmroExtendedTheme.colors.placeholderMuted,
        )
        PlaceholderBlock(
            modifier =
                Modifier
                    .fillMaxWidth(valueWidth)
                    .height(FACT_VALUE_HEIGHT),
            color = AmroExtendedTheme.colors.placeholder,
        )
    }
}

@Preview
@Composable
private fun DetailSkeletonPreview() {
    AmroTheme {
        Surface {
            DetailSkeleton(
                title = MovieDetailPreviewData.SKELETON_TITLE,
                content = MovieDetailPreviewData.SKELETON_CONTENT,
                bottomPadding = 0.dp,
            )
        }
    }
}
