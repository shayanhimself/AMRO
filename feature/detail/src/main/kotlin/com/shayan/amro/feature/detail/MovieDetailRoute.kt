package com.shayan.amro.feature.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme

/**
 * One movie's detail.
 *
 * @param sourceId the source that issued [movieId].
 * @param movieId the movie to show.
 * @param onBack leaves the screen, null where there is nowhere to go back to.
 */
@Composable
fun MovieDetailRoute(
    sourceId: String,
    movieId: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // An empty surface until the screen itself lands.
    Surface(modifier = modifier.fillMaxSize()) {}
}
