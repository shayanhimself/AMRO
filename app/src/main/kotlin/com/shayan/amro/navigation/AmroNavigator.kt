package com.shayan.amro.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * The only thing that mutates the back stack. Every rule that rewrites it lives here as a named
 * operation, so features keep receiving lambdas and knowing nothing about navigation.
 */
internal class AmroNavigator(
    private val backStack: NavBackStack<NavKey>,
) {
    /**
     * Opens movie [id], replacing the open movie rather than stacking on it.
     */
    fun openMovie(id: Int) {
        val key = MovieDetailKey(id)
        if (backStack.lastOrNull() is MovieDetailKey) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    /**
     * Pops the top entry, and does nothing when that entry is the only one left.
     *
     * `NavDisplay` requires a stack with something in it, so popping the last entry crashes the
     * next composition.
     */
    fun back() {
        if (backStack.size <= 1) return
        backStack.removeLastOrNull()
    }
}
