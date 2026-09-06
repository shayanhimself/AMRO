package com.shayan.amro.feature.trending.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.trending.R
import com.shayan.amro.feature.trending.ui.TrendingPreviewData
import com.shayan.amro.feature.trending.viewmodel.MovieRowUiState

/** The poster tile, at the 2:3 every source serves. */
internal val POSTER_WIDTH = 76.dp
internal val POSTER_HEIGHT = 114.dp

/** What sits between two genre names on a row. */
private const val GENRE_SEPARATOR = " · "

/**
 * One movie in the trending list: its poster, its title, and the genres it carries.
 *
 * The whole row is one node a screen reader reaches, because a poster and the title beside it are
 * one thing to select rather than three.
 *
 * @param row what the row renders.
 * @param onClick opens this movie.
 */
@Composable
internal fun MovieRow(
    row: MovieRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterTile(row = row)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val genreNames = row.genreLabels.map { stringResource(it) }
            Text(
                text = genreNames.joinToString(GENRE_SEPARATOR),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The poster, or the tile that stands in for one the source does not have.
 *
 * @param row the film the poster is described by.
 */
@Composable
private fun PosterTile(
    row: MovieRowUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(POSTER_WIDTH)
                .height(POSTER_HEIGHT)
                .clip(RoundedCornerShape(RadiusPrimitives.radius2))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        val posterUrl = row.posterUrl
        if (posterUrl == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
                modifier = Modifier.padding(Spacing.s2),
            ) {
                DsIcon(
                    glyph = Glyphs.MOVIE,
                    // The label below says what the tile is.
                    contentDescription = null,
                    size = 28.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.feature_trending_no_poster),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AsyncImage(
                model = posterUrl,
                contentDescription =
                    stringResource(
                        id = R.string.feature_trending_poster_description,
                        row.title,
                    ),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
private fun MovieRowPreview() {
    AmroTheme {
        Surface {
            MovieRow(
                row = TrendingPreviewData.ROWS.first(),
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
                onClick = {},
                modifier = Modifier.padding(Spacing.s4),
            )
        }
    }
}
