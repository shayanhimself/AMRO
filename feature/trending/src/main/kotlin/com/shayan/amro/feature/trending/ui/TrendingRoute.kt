package com.shayan.amro.feature.trending.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shayan.amro.feature.trending.viewmodel.TrendingContent
import com.shayan.amro.feature.trending.viewmodel.TrendingViewModel

/**
 * The trending movies list, stateful, and with the view model injected.
 *
 * @param selectedMovieId the movie shown beside the list, or null where the list is the only pane.
 * @param onMovieClick opens the movie the user selected.
 * @param onSelectFirstMovie opens the first movie on first composition, when adaptive layout has
 * two panes and nothing is selected yet.
 */
@Composable
fun TrendingRoute(
    selectedMovieId: String?,
    onMovieClick: (movieId: String) -> Unit,
    onSelectFirstMovie: ((movieId: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val viewModel: TrendingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onLaunch() }

    val firstMovieId = (uiState.content as? TrendingContent.Movies)?.rows?.firstOrNull()?.movieId
    SelectFirstMovieWhereNothingIsSelected(
        firstMovieId = firstMovieId,
        selectedMovieId = selectedMovieId,
        onSelectFirstMovie = onSelectFirstMovie,
    )

    TrendingScreen(
        uiState = uiState,
        selectedMovieId = selectedMovieId,
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

/**
 * Selects the first movie on first composition, if nothing is selected.
 * The selection itself will work only if adaptive layout has 2 panes.
 *
 * @param firstMovieId the row the list leads with, or null where it holds no rows.
 * @param selectedMovieId the movie already shown beside the list.
 * @param onSelectFirstMovie what opens it, or null where a pane beside the list is not shown.
 */
@Composable
private fun SelectFirstMovieWhereNothingIsSelected(
    firstMovieId: String?,
    selectedMovieId: String?,
    onSelectFirstMovie: ((movieId: String) -> Unit)?,
) {
    val selectFirstMovie by rememberUpdatedState(onSelectFirstMovie)
    LaunchedEffect(onSelectFirstMovie != null, selectedMovieId, firstMovieId) {
        if (selectedMovieId != null || firstMovieId == null) return@LaunchedEffect
        selectFirstMovie?.invoke(firstMovieId)
    }
}
