package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.ComponentShapes
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.detail.ui.MovieDetailPreviewData
import kotlinx.collections.immutable.ImmutableList

/**
 * The genres, as non-interactive chips.
 *
 * @param genreLabels what the chips read, in the order they are drawn.
 * @param description the same list as the one statement a screen reader hears.
 */
@Composable
internal fun GenreChips(
    genreLabels: ImmutableList<AmroText>,
    description: AmroText,
    modifier: Modifier = Modifier,
) {
    val spoken = description.resolve()
    FlowRow(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = spoken
            },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        genreLabels.forEach { label -> GenreChip(name = label.resolve()) }
    }
}

@Composable
private fun GenreChip(
    name: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = ComponentShapes.chip,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(horizontal = Spacing.s4)
                    .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview
@Composable
private fun GenreChipsPreview() {
    AmroTheme {
        Surface {
            GenreChips(
                genreLabels = MovieDetailPreviewData.GENRES,
                description = AmroText.Empty,
                modifier = Modifier.padding(Spacing.s4),
            )
        }
    }
}
