package com.shayan.amro.navigation

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

    AmroNavDisplay(
        backStack = backStack,
        navigator = navigator,
        modifier = modifier,
    )
}
