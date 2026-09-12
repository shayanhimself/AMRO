package com.shayan.amro.e2e

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.e2e.screens.LIST_TIMEOUT_MILLIS
import com.shayan.amro.e2e.screens.awaitFirstRowTitle
import com.shayan.amro.e2e.screens.firstRowTitle
import com.shayan.amro.e2e.screens.lastRowTitle
import com.shayan.amro.e2e.screens.rowCount
import com.shayan.amro.e2e.screens.swipeMovieListPastTheFirstRead
import com.shayan.amro.helpers.AppLauncher
import com.shayan.amro.helpers.FreshInstall
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@E2eTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DiscoverE2eTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val freshInstall = FreshInstall()

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    private val launcher = AppLauncher()

    @After
    fun tearDown() {
        launcher.close()
    }

    @Test
    fun theMovieListKeepsServingRowsAsItIsScrolled() {
        launcher.launch()

        val opening = composeRule.awaitFirstRowTitle()
        val openingRows = composeRule.rowCount()

        composeRule.swipeMovieListPastTheFirstRead()

        composeRule.waitUntil(LIST_TIMEOUT_MILLIS) { composeRule.rowCount() > openingRows }
        assertNotEquals(opening, composeRule.firstRowTitle(), "the list stayed where it opened")
        assertNotNull(composeRule.lastRowTitle())
    }
}
