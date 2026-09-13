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
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.component.PosterTile
import com.shayan.amro.core.ui.component.placeholder.PlaceholderBlock
import com.shayan.amro.core.ui.component.placeholder.nextWidth
import com.shayan.amro.core.ui.component.placeholder.pulsingAlpha
import com.shayan.amro.core.ui.component.placeholder.rememberPlaceholderPulseAlpha
import com.shayan.amro.core.ui.designsystem.theme.AmroExtendedTheme
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.detail.ui.MovieDetailPreviewData
import com.shayan.amro.feature.detail.viewmodel.MovieDetailContent
import kotlin.random.Random
import com.shayan.amro.core.ui.R as CoreUiR

private const val BADGE_LINE_WIDTH_FRACTION = 0.56f

private const val TAGLINE_LINE_WIDTH_FRACTION = 0.52f

/** How many lines the overview will become. */
private const val OVERVIEW_LINE_COUNT = 8

/** How wide a full line of the overview runs, as a share of the width. */
private val OVERVIEW_LINE_WIDTHS = 0.88f..0.97f

/** How wide the last line runs, short like the end of a paragraph. */
private val OVERVIEW_LAST_LINE_WIDTHS = 0.40f..0.65f

/** How many cells the grid will become. */
private const val FACT_COUNT = 4

/** How wide a fact's label runs, as a share of its column. */
private val FACT_LABEL_WIDTHS = 0.38f..0.58f

/** How wide a fact's value runs. */
private val FACT_VALUE_WIDTHS = 0.62f..0.80f

/** What every placeholder width is drawn from, fixed so the skeleton reads the same each time. */
private const val PLACEHOLDER_WIDTHS_SEED = 1000

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
    val alpha = rememberPlaceholderPulseAlpha()
    // The header goes edge to edge, the rest have is inset.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
    ) {
        SkeletonHeader(title = title, posterUrl = content.posterUrl, alpha = alpha)

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
                            .height(Spacing.s4)
                            .pulsingAlpha(alpha),
                    color = AmroExtendedTheme.colors.placeholder,
                )
            }
            if (content.genreLabels.isNotEmpty()) {
                GenreChips(
                    genreLabels = content.genreLabels,
                    description = content.genreDescription,
                )
            }
            AwaitedBody(alpha = alpha)
        }
    }
}

/**
 * The backdrop, the poster over it and the rating's placeholder.
 *
 * @param alpha the opacity the placeholders are drawn at.
 */
@Composable
private fun SkeletonHeader(
    title: String,
    posterUrl: String?,
    alpha: State<Float>,
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
                    .pulsingAlpha(alpha)
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
                        .height(Spacing.s6)
                        .pulsingAlpha(alpha),
                color = AmroExtendedTheme.colors.placeholder,
            )
        }
    }
}

/**
 * The movie overview, facts placeholders.
 *
 * @param alpha the opacity the placeholders are drawn at.
 */
@Composable
private fun AwaitedBody(
    alpha: State<Float>,
    modifier: Modifier = Modifier,
) {
    val loading = stringResource(CoreUiR.string.core_ui_loading)
    val overviewLineWidths = rememberOverviewLineWidths()
    val factWidths = rememberFactWidths()
    // One node for the whole region, so the wait is announced once and not per block.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .pulsingAlpha(alpha)
                .semantics(mergeDescendants = true) { contentDescription = loading },
    ) {
        // Overview placeholder lines.
        Column(
            modifier = Modifier.padding(top = Spacing.s1),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            overviewLineWidths.forEach { width ->
                PlaceholderBlock(
                    modifier =
                        Modifier
                            .fillMaxWidth(width)
                            .height(Spacing.s4),
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
            factWidths.chunked(FACTS_COLUMN_COUNT).forEach { row ->
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
                    .height(Spacing.s3),
            color = AmroExtendedTheme.colors.placeholderMuted,
        )
        PlaceholderBlock(
            modifier =
                Modifier
                    .fillMaxWidth(valueWidth)
                    .height(Spacing.s4),
            color = AmroExtendedTheme.colors.placeholder,
        )
    }
}

/**
 * The share of the width each overview line runs to, the last one is short like the end of a
 * paragraph.
 *
 * Widths are drawn from a fixed seed rather than from chance, so the skeleton keeps its shape.
 */
@Composable
private fun rememberOverviewLineWidths(): List<Float> =
    remember {
        val widths = Random(seed = PLACEHOLDER_WIDTHS_SEED)
        List(OVERVIEW_LINE_COUNT) { line ->
            val isLast = line == OVERVIEW_LINE_COUNT - 1
            widths.nextWidth(if (isLast) OVERVIEW_LAST_LINE_WIDTHS else OVERVIEW_LINE_WIDTHS)
        }
    }

/**
 * The share of its column each fact's label and value run to, in the order they are drawn.
 *
 * Drawn from a fixed seed, like the overview lines.
 */
@Composable
private fun rememberFactWidths(): List<Pair<Float, Float>> =
    remember {
        val widths = Random(seed = PLACEHOLDER_WIDTHS_SEED)
        List(FACT_COUNT) {
            widths.nextWidth(FACT_LABEL_WIDTHS) to widths.nextWidth(FACT_VALUE_WIDTHS)
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
