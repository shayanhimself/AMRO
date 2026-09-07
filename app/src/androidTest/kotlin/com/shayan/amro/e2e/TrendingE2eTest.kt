package com.shayan.amro.e2e

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.e2e.helpers.E2eTest
import com.shayan.amro.e2e.helpers.FreshInstall
import com.shayan.amro.e2e.screens.awaitFirstRowTitle
import com.shayan.amro.e2e.screens.lastRowTitle
import com.shayan.amro.e2e.screens.rowCount
import com.shayan.amro.e2e.screens.swipeTrendingListToEnd
import com.shayan.amro.flow.helpers.AppLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@E2eTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TrendingE2eTest {
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
    fun trendingMovieSetLoadsAndScrollsToItsEnd() {
        launcher.launch()

        composeRule.awaitFirstRowTitle()

        val lastIndex = composeRule.rowCount() - 1
        assertTrue(lastIndex > 0, "the list holds one row, so there is no end to scroll to")
        composeRule.swipeTrendingListToEnd()
        assertNotNull(composeRule.lastRowTitle())
    }
}
