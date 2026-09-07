package com.shayan.amro.feature.trending.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.component.MessagePanel
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.trending.R
import com.shayan.amro.feature.trending.component.FilterSheet
import com.shayan.amro.feature.trending.component.MovieRow
import com.shayan.amro.feature.trending.component.TrendingAppBar
import com.shayan.amro.feature.trending.component.TrendingNoticeBar
import com.shayan.amro.feature.trending.component.TrendingSkeleton
import com.shayan.amro.feature.trending.component.trendingListPadding
import com.shayan.amro.feature.trending.viewmodel.SelectionUiState
import com.shayan.amro.feature.trending.viewmodel.SortDirection
import com.shayan.amro.feature.trending.viewmodel.SortKey
import com.shayan.amro.feature.trending.viewmodel.TrendingContent
import com.shayan.amro.feature.trending.viewmodel.TrendingUiState
import com.shayan.amro.core.ui.R as CoreUiR

/**
 * Shows the list of trending movies, and lets the user filter and sort them. Stateless.
 *
 * @param uiState what to render.
 * @param onMovieClick opens one movie, by the provider that issued it and its id there.
 * @param onRefresh fetches the trending set again.
 * @param onToggleGenre selects or deselects one genre.
 * @param onSelectSortKey orders by another key.
 * @param onSelectSortDirection runs the ordering the other way.
 * @param onClearGenres drops every selected genre, keeping the order.
 * @param onReset drops the genres and returns the order to the default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrendingScreen(
    uiState: TrendingUiState,
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
    val listState = rememberLazyListState()

    ScrollToTopOnSelectionChange(selection = uiState.selection, listState = listState)

    Scaffold(
        modifier = modifier,
        topBar = {
            TrendingAppBar(
                activeSelectionCount = uiState.activeSelectionCount,
                onFilterClick = { isSheetOpen = true },
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            uiState.notice?.let { notice ->
                TrendingNoticeBar(notice = notice, onRetry = onRefresh)
            }
            when (val content = uiState.content) {
                TrendingContent.Skeleton -> {
                    TrendingSkeleton()
                }

                is TrendingContent.Movies -> {
                    // The gesture and its indicator are built only where there is a list to pull
                    // and a cache worth preserving, which is what keeps them off every other
                    // branch with no condition at the render site.
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRefresh,
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = trendingListPadding,
                            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
                        ) {
                            items(content.rows, key = { it.movieId }) { row ->
                                MovieRow(
                                    row = row,
                                    onClick = { onMovieClick(row.movieId) },
                                )
                            }
                        }
                    }
                }

                TrendingContent.EmptyFromFilter -> {
                    MessagePanel(
                        glyph = Glyphs.FILTER_ALT_OFF,
                        title = stringResource(R.string.feature_trending_empty_filter_title),
                        body = stringResource(R.string.feature_trending_empty_filter_body),
                        actionLabel = stringResource(R.string.feature_trending_clear_filters),
                        onAction = onClearGenres,
                        actionVariant = ButtonVariant.Tonal,
                        actionGlyph = Glyphs.FILTER_ALT_OFF,
                    )
                }

                is TrendingContent.Error -> {
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

/**
 * Returns [listState] to the first row whenever sort or filters change.
 *
 * @param selection what the list is narrowed and ordered by.
 * @param listState the list to return to the top.
 */
@Composable
private fun ScrollToTopOnSelectionChange(
    selection: SelectionUiState,
    listState: LazyListState,
) {
    // The first run is the screen opening, so it is skipped.
    var isFirstSelection by remember { mutableStateOf(true) }
    LaunchedEffect(selection) {
        if (isFirstSelection) {
            isFirstSelection = false
        } else {
            listState.scrollToItem(0)
        }
    }
}

@Preview
@Composable
private fun TrendingScreenPreview() {
    AmroTheme {
        TrendingScreenPreviewHost(state = TrendingPreviewData.LOADED)
    }
}

@Preview
@Composable
private fun TrendingScreenSkeletonPreview() {
    AmroTheme {
        TrendingScreenPreviewHost(state = TrendingPreviewData.SKELETON)
    }
}

@Preview
@Composable
private fun TrendingScreenErrorPreview() {
    AmroTheme {
        TrendingScreenPreviewHost(state = TrendingPreviewData.ERROR)
    }
}
