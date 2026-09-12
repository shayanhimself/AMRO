package com.shayan.amro.feature.discover.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.ui.component.MessagePanel
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.discover.R
import com.shayan.amro.feature.discover.component.DiscoverAppBar
import com.shayan.amro.feature.discover.component.DiscoverNoticeBar
import com.shayan.amro.feature.discover.component.DiscoverSkeleton
import com.shayan.amro.feature.discover.component.FilterSheet
import com.shayan.amro.feature.discover.component.MovieList
import com.shayan.amro.feature.discover.component.MovieRows
import com.shayan.amro.feature.discover.viewmodel.DiscoverBody
import com.shayan.amro.feature.discover.viewmodel.DiscoverContent
import com.shayan.amro.feature.discover.viewmodel.DiscoverUiState
import com.shayan.amro.core.ui.R as CoreUiR

private val discoverListPadding =
    PaddingValues(
        start = Spacing.s2,
        end = Spacing.s2,
        bottom = Spacing.s2,
    )

/**
 * Shows the movies a selection describes, and lets the user change that selection. Stateless.
 *
 * @param uiState what the app bar and the sheet render.
 * @param body what the list is putting on and above the screen.
 * @param rows the rows the list holds.
 * @param selectedMovieId the selected movie when adaptive layout has 2 panes.
 * @param onMovieClick opens one movie, by the provider that issued it and its id there.
 * @param onRefresh reads the head of the list again.
 * @param onToggleGenre selects or deselects one genre.
 * @param onSelectSortKey orders by another key.
 * @param onSelectSortDirection runs the ordering the other way.
 * @param onClearGenres drops every selected genre, keeping the order.
 * @param onReset drops the genres and returns the order to the default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoverScreen(
    uiState: DiscoverUiState,
    body: DiscoverBody,
    rows: MovieRows,
    selectedMovieId: String?,
    onMovieClick: (movieId: String) -> Unit,
    onRefresh: () -> Unit,
    onToggleGenre: (String) -> Unit,
    onSelectSortKey: (SortKey) -> Unit,
    onSelectSortDirection: (SortDirection) -> Unit,
    onClearGenres: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            DiscoverAppBar(
                activeSelectionCount = uiState.activeSelectionCount,
                onFilterClick = { isSheetOpen = true },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            body.notice?.let { notice ->
                DiscoverNoticeBar(notice = notice, onRetry = onRefresh)
            }
            when (val content = body.content) {
                DiscoverContent.Skeleton -> {
                    DiscoverSkeleton(modifier = Modifier.padding(discoverListPadding))
                }

                DiscoverContent.Movies -> {
                    MovieList(
                        rows = rows,
                        selectedMovieId = selectedMovieId,
                        isRefreshing = body.isRefreshing,
                        onRefresh = onRefresh,
                        onMovieClick = onMovieClick,
                        contentPadding = discoverListPadding,
                    )
                }

                DiscoverContent.EmptyFromFilter -> {
                    MessagePanel(
                        glyph = Glyphs.FILTER_ALT_OFF,
                        title = stringResource(R.string.feature_discover_empty_filter_title),
                        body = stringResource(R.string.feature_discover_empty_filter_body),
                        actionLabel = stringResource(R.string.feature_discover_clear_filters),
                        onAction = onClearGenres,
                        actionVariant = ButtonVariant.Tonal,
                        actionGlyph = Glyphs.FILTER_ALT_OFF,
                    )
                }

                is DiscoverContent.Error -> {
                    MessagePanel(
                        glyph = content.glyph,
                        title = content.title.resolve(),
                        actionLabel = stringResource(CoreUiR.string.core_ui_retry),
                        onAction = onRefresh,
                        glyphTint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (isSheetOpen) {
        FilterSheet(
            filter = uiState.filter,
            onToggleGenre = onToggleGenre,
            onSelectSortKey = onSelectSortKey,
            onSelectSortDirection = onSelectSortDirection,
            onReset = onReset,
            onDismiss = { isSheetOpen = false },
        )
    }
}

@Preview
@Composable
private fun DiscoverScreenPreview() {
    AmroTheme {
        DiscoverScreenPreviewHost(state = DiscoverPreviewData.LOADED)
    }
}

@Preview
@Composable
private fun DiscoverScreenSelectedPreview() {
    AmroTheme {
        DiscoverScreenPreviewHost(
            state = DiscoverPreviewData.LOADED,
            selectedMovieId = DiscoverPreviewData.ROWS.first().movieId,
        )
    }
}

@Preview
@Composable
private fun DiscoverScreenSkeletonPreview() {
    AmroTheme {
        DiscoverScreenPreviewHost(state = DiscoverPreviewData.SKELETON)
    }
}

@Preview
@Composable
private fun DiscoverScreenErrorPreview() {
    AmroTheme {
        DiscoverScreenPreviewHost(state = DiscoverPreviewData.ERROR)
    }
}
