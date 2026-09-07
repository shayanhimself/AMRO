package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.detail.ui.MovieDetailPreviewData
import com.shayan.amro.feature.detail.viewmodel.MovieFactUiState
import kotlinx.collections.immutable.ImmutableList

/** How many facts a row of the grid holds. */
private const val FACTS_COLUMN_COUNT = 2

/**
 * The facts under the overview of a movie.
 *
 * @param facts what the grid states, in the order it states them.
 */
@Composable
internal fun DetailFacts(
    facts: ImmutableList<MovieFactUiState>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        // Container to give inset padding, and space between the grid rows.
        Column(
            modifier = Modifier.padding(top = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            facts.chunked(FACTS_COLUMN_COUNT).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                    row.forEach { cell ->
                        Fact(fact = cell, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * One cell: a label, and the value under it.
 */
@Composable
private fun Fact(
    fact: MovieFactUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Spacing.s0_5),
    ) {
        Text(
            text = fact.label.resolve(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = fact.value.resolve(),
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (fact.disclosed) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Preview
@Composable
private fun DetailFactsPreview() {
    AmroTheme {
        Surface {
            DetailFacts(
                facts = MovieDetailPreviewData.FACTS,
                modifier = Modifier.padding(Spacing.s4),
            )
        }
    }
}

@Preview
@Composable
private fun DetailFactsUndisclosedPreview() {
    AmroTheme {
        Surface {
            DetailFacts(
                facts = MovieDetailPreviewData.UNDISCLOSED_FACTS,
                modifier = Modifier.padding(Spacing.s4),
            )
        }
    }
}
