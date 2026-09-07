package com.shayan.amro.e2e

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.testing.string
import com.shayan.amro.e2e.screens.awaitFirstRowTitle
import com.shayan.amro.feature.detail.R
import com.shayan.amro.helpers.AppLauncher
import com.shayan.amro.helpers.FreshInstall
import com.shayan.amro.helpers.awaitText
import com.shayan.amro.helpers.clickWhenStill
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** How long the live record may take, which is one request on a connection already open. */
private const val DETAIL_TIMEOUT_MILLIS = 30_000L

@E2eTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DetailE2eTest {
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
    fun openingAMovieShowsItsRecord() {
        launcher.launch()

        val title = composeRule.awaitFirstRowTitle()
        composeRule.clickWhenStill(hasText(title))
        composeRule.awaitText(string(R.string.feature_detail_fact_runtime), DETAIL_TIMEOUT_MILLIS)
        composeRule.onAllNodesWithText(title).assertCountEquals(1)
    }
}
