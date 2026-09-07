package com.shayan.amro.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shayan.amro.core.ui.R
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing

/** The glyph the tile draws where a source served no poster. */
private val PLACEHOLDER_GLYPH_SIZE = 28.dp

/**
 * A movie's poster, or the tile that stands in for one the source does not have.
 * The caller sizes it.
 *
 * @param posterUrl the address to load, or null where the source offers no poster.
 * @param title the film the poster is described by.
 */
@Composable
fun PosterTile(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(RadiusPrimitives.radius2))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
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
                    size = PLACEHOLDER_GLYPH_SIZE,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.core_ui_no_poster),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AsyncImage(
                model = posterUrl,
                contentDescription = stringResource(R.string.core_ui_poster_description, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
private fun PosterTilePreview() {
    AmroTheme {
        Surface {
            PosterTile(
                posterUrl = "https://image.example/w185/poster.jpg",
                title = "The Mongoose",
                modifier =
                    Modifier
                        .padding(Spacing.s4)
                        .width(76.dp)
                        .height(114.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PosterTileWithoutPosterPreview() {
    AmroTheme {
        Surface {
            PosterTile(
                posterUrl = null,
                title = "The Mongoose",
                modifier =
                    Modifier
                        .padding(Spacing.s4)
                        .width(76.dp)
                        .height(114.dp),
            )
        }
    }
}
