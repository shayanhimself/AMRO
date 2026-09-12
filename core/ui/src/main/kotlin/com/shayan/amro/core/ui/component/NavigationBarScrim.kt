package com.shayan.amro.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/** How much of the scrim's opacity is left at its top edge, so it fades in. */
private const val NAVIGATION_BAR_SCRIM_TOP_SHARE = 0.1f

/**
 * The fade behind the navigation bar, so its handle stays readable over content scrolling under it.
 */
@Composable
fun NavigationBarScrim(modifier: Modifier = Modifier) {
    val scrim = MaterialTheme.colorScheme.scrim
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scrim.copy(alpha = scrim.alpha * NAVIGATION_BAR_SCRIM_TOP_SHARE),
                            scrim,
                        ),
                    ),
                ),
    )
}
