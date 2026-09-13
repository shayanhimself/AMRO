package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The fade carrying the bottom of the backdrop into the page under it.
 */
@Composable
internal fun BackdropFade(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier =
            modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.4f to Color.Transparent,
                    1f to background,
                ),
            ),
    )
}
