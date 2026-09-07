package com.shayan.amro.flow

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.network.sources.tmdb.TmdbConfig
import com.shayan.amro.core.testing.fixture.tmdb.TmdbFixture
import com.shayan.amro.core.testing.quantityString
import com.shayan.amro.core.testing.string
import com.shayan.amro.di.NetworkModule
import com.shayan.amro.feature.trending.R
import com.shayan.amro.flow.helpers.AppLauncher
import com.shayan.amro.flow.helpers.awaitContentDescription
import com.shayan.amro.flow.helpers.awaitText
import com.shayan.amro.flow.helpers.clickWhenStill
import com.shayan.amro.wire.LocalTmdb
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.shayan.amro.core.ui.R as CoreUiR

/** The most popular movie the recordings hold, which the default ordering puts first. */
private const val MOST_POPULAR_TITLE = "Spider-Man: Brand New Day"

/** How far down the list the test scrolls before the screen is built again under it. */
private const val SCROLL_TO_INDEX = 12

/**
 * What the trending screen does on a device, over the app's own graph and a local server.
 */
@HiltAndroidTest
@UninstallModules(NetworkModule::class)
@RunWith(AndroidJUnit4::class)
class TrendingSelectionFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val localTmdb = LocalTmdb()

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    private val launcher = AppLauncher()

    /** The address the app resolves its endpoints against, pointed at the local server. */
    @BindValue
    @JvmField
    val tmdbConfig: TmdbConfig = localTmdb.config

    @Before
    fun setUp() {
        hiltRule.inject()
        mockTrendingMoviesResponse()
    }

    @After
    fun tearDown() {
        launcher.close()
    }

    @Test
    fun aSelectionAndAScrollPositionSurviveRecreation() {
        val scenario = launcher.launch()

        composeRule.awaitContentDescription(loading())
        composeRule.awaitText(MOST_POPULAR_TITLE)
        composeRule.onAllNodes(hasContentDescription(loading())).assertCountEquals(0)

        composeRule.clickWhenStill(hasContentDescription(filterAction()))
        composeRule.clickWhenStill(hasText(genreLabel()))
        composeRule.awaitContentDescription(filterAction(count = 1))
        // Back is what closes the sheet on a compact width. Compose's test API has no press.
        Espresso.pressBack()
        composeRule.waitUntil {
            composeRule.onAllNodes(hasText(sheetTitle())).fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNode(hasScrollAction()).performScrollToIndex(SCROLL_TO_INDEX)
        val topRowBefore = topRowTitle()

        // The screen refreshes whenever it opens.
        mockTrendingMoviesResponse()
        // A configuration change is the only teardown an in-process instrumented test can provoke.
        scenario.recreate()

        composeRule.awaitContentDescription(filterAction(count = 1))
        assertEquals(topRowBefore, topRowTitle())
    }

    /** Answers one whole fetch of the trending movies set, a page at a time. */
    private fun mockTrendingMoviesResponse() {
        TmdbFixture.Trending.entries.forEach { page ->
            localTmdb.enqueueJson(page.json)
        }
    }

    /** The title of the row the list is scrolled to, read the way a screen reader reaches it. */
    private fun topRowTitle(): String {
        composeRule.waitForIdle()
        return composeRule
            .onNode(hasScrollAction())
            .fetchSemanticsNode()
            .children
            .first()
            .config[SemanticsProperties.Text]
            .first()
            .text
    }
}

/**
 * What the filter action is called, read the way a screen reader reaches it.
 *
 * @param count how many genres are selected, plus one for a sort away from the default.
 */
private fun filterAction(count: Int = 0): String =
    if (count == 0) {
        string(R.string.feature_trending_filter_action)
    } else {
        quantityString(R.plurals.feature_trending_filter_action_active, count, count)
    }

private fun genreLabel(): String = string(CoreUiR.string.core_ui_genre_comedy)

private fun sheetTitle(): String = string(R.string.feature_trending_sheet_title)

/** What the placeholders a first load draws are announced as. */
private fun loading(): String = string(CoreUiR.string.core_ui_loading)
