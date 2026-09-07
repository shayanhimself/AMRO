package com.shayan.amro.flow

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.di.RemoteSourceModule
import com.shayan.amro.core.testing.fake.FakeMovieRemoteDataSource
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.quantityString
import com.shayan.amro.core.testing.string
import com.shayan.amro.feature.trending.R
import com.shayan.amro.helpers.AppLauncher
import com.shayan.amro.helpers.FreshInstall
import com.shayan.amro.helpers.awaitContentDescription
import com.shayan.amro.helpers.awaitText
import com.shayan.amro.helpers.clickWhenStill
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

/**
 * More rows than a screen holds, and enough carrying any one genre that filtering down to it
 * still leaves a list longer than [SCROLL_TO_INDEX].
 */
private const val TRENDING_SET_SIZE = 100

/** How far down the list the test scrolls before the screen is built again under it. */
private const val SCROLL_TO_INDEX = 12

/**
 * What the trending screen does on a device, over the app's own graph and a fake source.
 */
@HiltAndroidTest
@UninstallModules(RemoteSourceModule::class)
@RunWith(AndroidJUnit4::class)
class TrendingSelectionFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val freshInstall = FreshInstall()

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    private val launcher = AppLauncher()

    private val trendingMovies = MovieFixture.trendingSet(TRENDING_SET_SIZE)

    private val remote = FakeMovieRemoteDataSource()

    /** The source the app reads movies from. */
    @BindValue
    @JvmField
    val movieRemoteDataSource: MovieRemoteDataSource = remote

    @Before
    fun setUp() {
        hiltRule.inject()
        remote.alwaysAnswerTrending(NetworkResult.Success(trendingMovies))
    }

    @After
    fun tearDown() {
        launcher.close()
    }

    @Test
    fun aSelectionAndAScrollPositionSurviveRecreation() {
        val scenario = launcher.launch()

        composeRule.awaitText(trendingMovies.first().title)

        composeRule.clickWhenStill(hasContentDescription(filterAction()))
        composeRule.clickWhenStill(genreChip())
        composeRule.awaitContentDescription(filterAction(count = 1))
        // Back is what closes the sheet on a compact width. Compose's test API has no press.
        Espresso.pressBack()
        composeRule.waitUntil {
            composeRule.onAllNodes(hasText(sheetTitle())).fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNode(hasScrollAction()).performScrollToIndex(SCROLL_TO_INDEX)
        val topRowBefore = topRowTitle()

        // A configuration change is the only teardown an in-process instrumented test can provoke.
        scenario.recreate()

        composeRule.awaitContentDescription(filterAction(count = 1))
        assertEquals(topRowBefore, topRowTitle())
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

/** The chip that filters on one genre. */
private fun genreChip(): SemanticsMatcher =
    hasText(string(CoreUiR.string.core_ui_genre_comedy)) and
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

private fun sheetTitle(): String = string(R.string.feature_trending_sheet_title)
