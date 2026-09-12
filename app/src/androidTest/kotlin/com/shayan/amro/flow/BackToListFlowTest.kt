package com.shayan.amro.flow

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.di.RemoteSourceModule
import com.shayan.amro.core.testing.fake.FakeMovieRemoteDataSource
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.string
import com.shayan.amro.feature.detail.R
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How long after leaving the detail the test taps the list. The list is fully on screen by then,
 * which is when a user taps it, but Navigation 3's default 700 ms crossfade would still be running.
 */
private const val TAP_AFTER_BACK_MILLIS = 400L

/**
 * Leaving a movie for the list, over the app's own graph and a fake source. Runs on a compact
 * width, where the list and the detail take the screen in turn.
 */
@HiltAndroidTest
@UninstallModules(RemoteSourceModule::class)
@RunWith(AndroidJUnit4::class)
class BackToListFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val freshInstall = FreshInstall()

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    private val launcher = AppLauncher()

    private val trendingMovies = MovieFixture.trendingSet(count = 3)

    private val remote = FakeMovieRemoteDataSource()

    /** The source the app reads movies from. */
    @BindValue
    @JvmField
    val movieRemoteDataSource: MovieRemoteDataSource = remote

    @Before
    fun setUp() {
        hiltRule.inject()
        remote.alwaysAnswerTrending(NetworkResult.Success(trendingMovies))
        remote.alwaysAnswerMovieDetail(NetworkResult.Success(MovieFixture.detail()))
    }

    @After
    fun tearDown() {
        composeRule.mainClock.autoAdvance = true
        launcher.close()
    }

    @Test
    fun theListTakesATapAsSoonAsItIsBack() {
        launcher.launch()

        composeRule.awaitText(trendingMovies[0].title)
        composeRule.clickWhenStill(hasText(trendingMovies[0].title))
        composeRule.awaitContentDescription(backAction())

        // The clock is held so the tap lands at a known point after back, not after the motion.
        composeRule.mainClock.autoAdvance = false
        composeRule.onNode(hasContentDescription(backAction())).performClick()
        composeRule.mainClock.advanceTimeBy(TAP_AFTER_BACK_MILLIS)
        composeRule.onNode(hasText(trendingMovies[1].title)).performClick()
        composeRule.mainClock.autoAdvance = true

        // The tap opened the second movie, rather than landing on the detail on its way out.
        composeRule.awaitContentDescription(backAction())
        composeRule.waitUntil { trendingMovies[1].id in remote.requestedIds }
    }
}

/** What the detail's back affordance is called, read the way a screen reader reaches it. */
private fun backAction(): String = string(R.string.feature_detail_back)
