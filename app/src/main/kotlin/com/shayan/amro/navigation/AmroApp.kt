package com.shayan.amro.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * The app's root composable.
 */
@Composable
fun AmroApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TrendingKey)
    val navigator = remember(backStack) { AmroNavigator(backStack) }

    // The app's root surface, which is the background for every screen.
    // Needed for adaptive layout's detail pane.
    Surface(modifier = modifier.fillMaxSize()) {
        AmroNavDisplay(
            backStack = backStack,
            navigator = navigator,
        )
    }
}
