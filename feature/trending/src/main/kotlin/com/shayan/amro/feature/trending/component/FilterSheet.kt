package com.shayan.amro.feature.trending.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.component.ChipVariant
import com.shayan.amro.core.ui.designsystem.component.DsButton
import com.shayan.amro.core.ui.designsystem.component.DsChip
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.trending.R
import com.shayan.amro.feature.trending.ui.TrendingPreviewData
import com.shayan.amro.feature.trending.viewmodel.ChipUiState
import com.shayan.amro.feature.trending.viewmodel.FilterUiState
import com.shayan.amro.feature.trending.viewmodel.SortDirection
import com.shayan.amro.feature.trending.viewmodel.SortKey

/**
 * Bottom sheet that lets the user filter and sort the trending movies list.
 *
 * @param filter what the sheet offers, and what it currently keeps.
 * @param onToggleGenre selects or deselects one genre.
 * @param onSelectSortKey orders by another key.
 * @param onSelectSortDirection runs the ordering the other way.
 * @param onReset drops the genres and returns the order to the default.
 * @param onDismiss closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    filter: FilterUiState,
    onToggleGenre: (String) -> Unit,
    onSelectSortKey: (SortKey) -> Unit,
    onSelectSortDirection: (SortDirection) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        FilterSheetContent(
            filter = filter,
            onToggleGenre = onToggleGenre,
            onSelectSortKey = onSelectSortKey,
            onSelectSortDirection = onSelectSortDirection,
            onReset = onReset,
        )
    }
}

/**
 * Content of the sheet, for previews and tests.
 */
@Composable
internal fun FilterSheetContent(
    filter: FilterUiState,
    onToggleGenre: (String) -> Unit,
    onSelectSortKey: (SortKey) -> Unit,
    onSelectSortDirection: (SortDirection) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.s5, end = Spacing.s2, bottom = Spacing.s2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feature_trending_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DsButton(
                text = stringResource(R.string.feature_trending_sheet_reset),
                onClick = onReset,
                variant = ButtonVariant.Text,
            )
        }

        Column(
            modifier =
                Modifier
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.s5),
            verticalArrangement = Arrangement.spacedBy(Spacing.s5),
        ) {
            SheetSection(headingRes = R.string.feature_trending_sheet_genre_heading) {
                ChipRow(chips = filter.genres, onClick = onToggleGenre)
            }
            SheetSection(headingRes = R.string.feature_trending_sheet_sort_heading) {
                ChipRow(chips = filter.sortKeys, onClick = onSelectSortKey)
            }
            SheetSection(headingRes = R.string.feature_trending_sheet_direction_heading) {
                ChipRow(chips = filter.directions, onClick = onSelectSortDirection)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = Spacing.s4),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text =
                stringResource(
                    R.string.feature_trending_sheet_count,
                    filter.shownCount,
                    filter.totalCount,
                ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .padding(
                        start = Spacing.s5,
                        end = Spacing.s5,
                        top = Spacing.s4,
                        bottom = Spacing.s6,
                    )
                    // The count is the one place the result of a choice is stated, and it is
                    // beside the chip that changed it, so it announces politely rather than
                    // interrupting that chip.
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * The chips of one section.
 *
 * @param chips the choices the section offers.
 * @param onClick reports the choice a chip carries.
 */
@Composable
private fun <T> ChipRow(
    chips: List<ChipUiState<T>>,
    onClick: (T) -> Unit,
) {
    chips.forEach { chip ->
        DsChip(
            label = chip.label.resolve(),
            onClick = { onClick(chip.value) },
            variant = ChipVariant.Filter,
            selected = chip.isSelected,
            leadingGlyph = chip.glyph,
        )
    }
}

/**
 * One heading and the chips under it.
 *
 * @param headingRes what the section is called.
 * @param chips the choices it offers.
 */
@Composable
private fun SheetSection(
    @StringRes headingRes: Int,
    chips: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text(
            text = stringResource(headingRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) { chips() }
    }
}

@Preview
@Composable
private fun FilterSheetContentPreview() {
    AmroTheme {
        Surface {
            FilterSheetContent(
                filter = TrendingPreviewData.NARROWED.filter,
                onToggleGenre = {},
                onSelectSortKey = {},
                onSelectSortDirection = {},
                onReset = {},
            )
        }
    }
}
