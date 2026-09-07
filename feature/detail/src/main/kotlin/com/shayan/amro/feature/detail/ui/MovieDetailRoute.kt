package com.shayan.amro.feature.detail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shayan.amro.feature.detail.viewmodel.MovieDetailViewModel

/**
 * One movie's detail route, stateful, and with the view model built from the id.
 *
 * @param movieId the movie to show.
 * @param onBack leaves the screen, null where there is nowhere to go back to.
 */
@Composable
fun MovieDetailRoute(
    movieId: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val viewModel: MovieDetailViewModel =
        hiltViewModel<MovieDetailViewModel, MovieDetailViewModel.Factory>(
            key = movieId,
        ) { factory ->
            factory.create(movieId)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) { viewModel.onLaunch() }

    MovieDetailScreen(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onOpenImdb = uriHandler::openUri,
        onBack = onBack,
        modifier = modifier,
    )
}
