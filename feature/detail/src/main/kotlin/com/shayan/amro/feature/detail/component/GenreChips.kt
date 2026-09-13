package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
    val shape = ComponentShapes.chip
    // Not using DsChip because it is interactive, and we want a non-interactive chip.
    Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .height(32.dp)
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(horizontal = Spacing.s4)
                .wrapContentHeight(Alignment.CenterVertically),
    )
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
