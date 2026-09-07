package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shayan.amro.core.ui.component.PosterTile
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.detail.R
import com.shayan.amro.feature.detail.viewmodel.HeaderBadgeUiState

/** The glyph a panel draws where a source served no backdrop. */
private val BACKDROP_GLYPH_SIZE = 36.dp

/** The star beside a rating, and the clock on a status pill. */
private val RATING_GLYPH_SIZE = 20.dp
private val STATUS_GLYPH_SIZE = 18.dp

/** The pill the status reads in. */
private val STATUS_PILL_HEIGHT = 32.dp

/** How much scrim the top of the backdrop carries, so the back action over it stays legible. */
private const val BACKDROP_SCRIM_ALPHA = 0.58f

/**
 * Where the scrim has thinned to [BACKDROP_SCRIM_MID_ALPHA] and where it has gone, as shares of the
 * backdrop's height. The knee is what leaves the art readable under an edge dark enough to carry
 * the back action.
 */
private const val BACKDROP_SCRIM_MID = 0.34f
private const val BACKDROP_SCRIM_MID_ALPHA = 0.08f
private const val BACKDROP_SCRIM_END = 0.56f

/**
 * The backdrop, the poster overlapping it, and the rating or the status beside them.
 *
 * @param title the film, which describes the poster.
 * @param posterUrl null when the source offers no poster.
 * @param backdropUrl null when the source offers no backdrop.
 * @param badge the rating, or the status that explains why there is none. Null for a source
 * reporting neither.
 */
@Composable
internal fun DetailHeader(
    title: String,
    posterUrl: String?,
    backdropUrl: String?,
    badge: HeaderBadgeUiState?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().height(HEADER_HEIGHT)) {
        Backdrop(backdropUrl = backdropUrl, modifier = Modifier.align(Alignment.TopStart))
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
            if (badge != null) {
                HeaderBadge(badge = badge, modifier = Modifier.padding(bottom = Spacing.s2))
            }
        }
    }
}

/**
 * The background art the screen opens on.
 * It is decorative: the poster beside it already names the film.
 */
@Composable
private fun Backdrop(
    backdropUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BACKDROP_HEIGHT)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // The gradient is drawn over the art, so the back action is legible.
        Box(
            modifier =
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = BACKDROP_SCRIM_ALPHA),
                        BACKDROP_SCRIM_MID to
                            Color.Black.copy(alpha = BACKDROP_SCRIM_MID_ALPHA),
                        BACKDROP_SCRIM_END to Color.Transparent,
                    ),
                ),
        )
        // Backdrop fades into the page under it.
        BackdropFade()
    }
}

/**
 * The rating, or the movie production status that takes its slot.
 */
@Composable
private fun HeaderBadge(
    badge: HeaderBadgeUiState,
    modifier: Modifier = Modifier,
) {
    when (badge) {
        is HeaderBadgeUiState.Rating -> Rating(badge = badge, modifier = modifier)
        is HeaderBadgeUiState.Status -> StatusPill(badge = badge, modifier = modifier)
    }
}

/** A rating, and how many votes produced it. */
@Composable
private fun Rating(
    badge: HeaderBadgeUiState.Rating,
    modifier: Modifier = Modifier,
) {
    val description = badge.description.resolve()
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DsIcon(
            glyph = Glyphs.STAR,
            contentDescription = null,
            size = RATING_GLYPH_SIZE,
            filled = true,
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = badge.rating.resolve(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        badge.voteCount?.let { votes ->
            Text(
                text = votes.resolve(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Where the movie sits in its release cycle, as a word in a pill rather than a tint. */
@Composable
private fun StatusPill(
    badge: HeaderBadgeUiState.Status,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(STATUS_PILL_HEIGHT),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DsIcon(glyph = Glyphs.SCHEDULE, contentDescription = null, size = STATUS_GLYPH_SIZE)
            Text(
                text = badge.label.resolve(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview
@Composable
private fun DetailHeaderPreview() {
    AmroTheme {
        Surface {
            DetailHeader(
                title = "Project Hail Mary",
                posterUrl = "https://image.example/w500/poster.jpg",
                backdropUrl = "https://image.example/w1280/backdrop.jpg",
                badge =
                    HeaderBadgeUiState.Rating(
                        rating = AmroText.Raw("8.6"),
                        voteCount = AmroText.Raw("7,472"),
                        description = AmroText.Empty,
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun DetailHeaderWithStatusPreview() {
    AmroTheme {
        Surface {
            DetailHeader(
                title = "Avengers: Doomsday",
                posterUrl = "https://image.example/w500/poster.jpg",
                backdropUrl = "https://image.example/w1280/backdrop.jpg",
                badge =
                    HeaderBadgeUiState.Status(
                        AmroText.Resource(R.string.feature_detail_status_post_production),
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun DetailHeaderWithoutArtPreview() {
    AmroTheme {
        Surface {
            DetailHeader(
                title = "The Mongoose",
                posterUrl = null,
                backdropUrl = null,
                badge = null,
            )
        }
    }
}
