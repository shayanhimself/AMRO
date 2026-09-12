package com.shayan.amro.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

private const val FIRST_MOVIE = "603"
private const val SECOND_MOVIE = "604"

class AmroNavigatorTest {
    private val backStack = NavBackStack<NavKey>(DiscoverKey)
    private val navigator = AmroNavigator(backStack)

    @Test
    fun `opening a movie from the list puts it over the list`() {
        navigator.openMovie(FIRST_MOVIE)

        assertEquals(listOf(DiscoverKey, MovieDetailKey(FIRST_MOVIE)), backStack.toList())
    }

    @Test
    fun `opening a second movie replaces the open one rather than stacking on it`() {
        navigator.openMovie(FIRST_MOVIE)
        navigator.openMovie(SECOND_MOVIE)

        assertEquals(listOf(DiscoverKey, MovieDetailKey(SECOND_MOVIE)), backStack.toList())
    }

    @Test
    fun `opening the movie that is already open leaves one movie open`() {
        navigator.openMovie(FIRST_MOVIE)
        navigator.openMovie(FIRST_MOVIE)

        assertEquals(listOf(DiscoverKey, MovieDetailKey(FIRST_MOVIE)), backStack.toList())
    }

    @Test
    fun `going back from a movie returns to the list`() {
        navigator.openMovie(FIRST_MOVIE)

        navigator.back()

        assertEquals(listOf(DiscoverKey), backStack.toList())
    }

    @Test
    fun `going back from the last entry leaves it in place`() {
        navigator.back()

        assertEquals(listOf(DiscoverKey), backStack.toList())
    }
}
