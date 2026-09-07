package com.shayan.amro.feature.trending.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.component.PosterTile
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.trending.ui.TrendingPreviewData
import com.shayan.amro.feature.trending.viewmodel.MovieRowUiState

/** The poster tile, at the 2:3 every source serves. */
internal val POSTER_WIDTH = 76.dp
internal val POSTER_HEIGHT = 114.dp
internal val MOVIE_ROW_PADDING = Spacing.s2

/**
 * The color of background, title and the poster ring
 *
 * @property background the row's own fill.
 * @property title the color of the title.
 * @property ring the outline over the poster, or null where the row is not the selected one.
 */
@Immutable
private data class MovieRowColors(
    val background: Color,
    val title: Color,
    val ring: Color?,
)

/**
 * One movie in the trending list: its poster, its title, and the genres it carries.
 *
 * The whole row is one node a screen reader reaches, because a poster and the title beside it are
 * one thing to select rather than three.
 *
 * @param row what the row renders.
 * @param isSelected whether this is the movie the detail pane is showing.
 * @param onClick opens this movie.
 */
@Composable
internal fun MovieRow(
    row: MovieRowUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = movieRowColors(isSelected)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusPrimitives.radius3))
                .background(colors.background)
                .selectable(selected = isSelected, onClick = onClick)
                .semantics(mergeDescendants = true) {}
                .padding(MOVIE_ROW_PADDING),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowPoster(row = row, ring = colors.ring)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.title,
            )
            Text(
                text = row.genres.resolve(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The movie's poster, ringed where the row is the selected one.
 *
 * @param row the movie the poster stands for.
 * @param ring poster's outline, or null where the row is not the selected one.
 */
@Composable
private fun RowPoster(
    row: MovieRowUiState,
    ring: Color?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(POSTER_WIDTH)
                .height(POSTER_HEIGHT),
    ) {
        PosterTile(
            posterUrl = row.posterUrl,
            title = row.title,
            modifier = Modifier.fillMaxSize(),
        )
        if (ring != null) {
            // The ring is drawn over the tile rather than around it, so the poster keeps the size
            // it has in every other row.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .border(
                            width = 3.dp,
                            color = ring,
                            shape = RoundedCornerShape(RadiusPrimitives.radius2),
                        ),
            )
        }
    }
}

/**
 * @param isSelected whether this is the movie the detail pane is showing.
 * @return The color of background, title and the poster ring.
 */
@Composable
private fun movieRowColors(isSelected: Boolean): MovieRowColors =
    if (isSelected) {
        MovieRowColors(
            background = MaterialTheme.colorScheme.surfaceContainer,
            title = MaterialTheme.colorScheme.primary,
            ring = MaterialTheme.colorScheme.primary,
        )
    } else {
        MovieRowColors(
            background = Color.Transparent,
            title = MaterialTheme.colorScheme.onSurface,
            ring = null,
        )
    }

@Preview
@Composable
private fun MovieRowPreview() {
    AmroTheme {
        Surface {
            MovieRow(
                row = TrendingPreviewData.ROWS.first(),
                isSelected = false,
                onClick = {},
                modifier = Modifier.padding(Spacing.s4),
            )
        }
    }
}

@Preview
@Composable
private fun MovieRowSelectedPreview() {
    AmroTheme {
        Surface {
            MovieRow(
                row = TrendingPreviewData.ROWS.first(),
                isSelected = true,
                onClick = {},
                modifier = Modifier.padding(Spacing.s4),
            )
        }
    }
}

@Preview
@Composable
private fun MovieRowWithoutPosterPreview() {
    AmroTheme {
        Surface {
            MovieRow(
                row = TrendingPreviewData.ROW_WITHOUT_POSTER,
                isSelected = false,
                onClick = {},
                modifier = Modifier.padding(Spacing.s4),
            )
        }
    }
}
