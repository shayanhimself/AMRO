package com.shayan.amro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import com.shayan.amro.core.ui.component.NavigationBarScrim

/**
 * The app's root composable.
 */
@Composable
fun AmroApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TrendingKey)
    val navigator = remember(backStack) { AmroNavigator(backStack) }

    // The app's root surface, which is the background for every screen.
    Surface(modifier = modifier.fillMaxSize()) {
        Box {
            AmroNavDisplay(
                backStack = backStack,
                navigator = navigator,
            )
            NavigationBarScrim(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
