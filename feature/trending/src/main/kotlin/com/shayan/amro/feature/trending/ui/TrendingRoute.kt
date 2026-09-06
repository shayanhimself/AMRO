package com.shayan.amro.feature.trending.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.trending.viewmodel.TrendingViewModel

/**
 * The trending movies list, stateful, and with the view model injected.
 *
 * @param onMovieClick opens the movie the user selected.
 */
@Composable
fun TrendingRoute(
    onMovieClick: (sourceId: String, movieId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TrendingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onLaunch() }

    TrendingScreen(
        uiState = uiState,
        onMovieClick = onMovieClick,
        onRefresh = viewModel::onRefresh,
        onToggleGenre = viewModel::onToggleGenre,
        onSelectSortKey = viewModel::onSelectSortKey,
        onSelectSortDirection = viewModel::onSelectSortDirection,
        onClearGenres = viewModel::onClearGenres,
        onReset = viewModel::onReset,
        modifier = modifier,
    )
}
