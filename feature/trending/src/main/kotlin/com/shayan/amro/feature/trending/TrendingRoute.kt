package com.shayan.amro.feature.trending

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme

/**
 * The trending movies list.
 *
 * @param onMovieClick opens the movie the user selected.
 */
@Composable
fun TrendingRoute(
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // An empty surface until the list itself lands
    Surface(modifier = modifier.fillMaxSize()) {}
}

@Preview
@Composable
private fun TrendingRoutePreview() {
    AmroTheme {
        TrendingRoute(onMovieClick = {})
    }
}
