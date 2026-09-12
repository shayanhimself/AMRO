package com.shayan.amro.feature.discover.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.discover.ui.DiscoverPreviewData

/**
 * The movies one selection produced, a page at a time.
 *
 * The pull gesture and its indicator live here rather than around the whole body, because they
 * belong where there is a list to pull and a cache worth preserving.
 *
 * @param rows the rows the list holds.
 * @param selectedMovieId the movie shown beside the list, or null where the list is the only pane.
 * @param isRefreshing whether a read of the head of the list is running.
 * @param onRefresh reads the head of the list again.
 * @param onMovieClick opens one movie.
 * @param contentPadding what the list keeps clear around its rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MovieList(
    rows: MovieRows,
    selectedMovieId: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMovieClick: (movieId: String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            items(count = rows.count, key = rows::keyAt) { index ->
                val row = rows[index] ?: return@items
                MovieRow(
                    row = row,
                    isSelected = row.movieId == selectedMovieId,
                    onClick = { onMovieClick(row.movieId) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun MovieListPreview() {
    AmroTheme {
        Surface {
            MovieList(
                rows = movieRows(DiscoverPreviewData.ROWS),
                selectedMovieId = null,
                isRefreshing = false,
                onRefresh = {},
                onMovieClick = {},
                contentPadding = PaddingValues(),
            )
        }
    }
}
