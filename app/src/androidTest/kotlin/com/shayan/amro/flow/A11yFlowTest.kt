package com.shayan.amro.flow

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesChecks
import com.google.android.apps.common.testing.accessibility.framework.checks.ImageContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.shayan.amro.core.network.MovieRemoteDataSource
import com.shayan.amro.core.network.NetworkResult
import com.shayan.amro.core.network.di.RemoteSourceModule
import com.shayan.amro.core.testing.fake.FakeMovieRemoteDataSource
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.string
import com.shayan.amro.feature.detail.R
import com.shayan.amro.helpers.AppLauncher
import com.shayan.amro.helpers.FreshInstall
import com.shayan.amro.helpers.awaitText
import com.shayan.amro.helpers.clickWhenStill
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The checks read the node tree through a platform API that arrived in Android 14, below which
// the framework has nothing to hand them.
private const val ACCESSIBILITY_CHECKS_MIN_SDK = 34

/** More rows than a screen holds, so the row the test opens is one it has to scroll to. */
private const val TRENDING_SET_SIZE = 40

/** The row the test opens, far enough down that reaching it scrolls the list. */
private const val DETAIL_ROW_INDEX = 20

/**
 * Whether every state the app puts on screen survives the Accessibility Test Framework's checks: a
 * label on everything a service can reach, text that meets contrast, tappable targets at the
 * minimum size, and a traversal order that reads the screen the way it looks.
 *
 * The checks run over the accessibility node tree the platform builds, which is what TalkBack is
 * handed, rather than over the semantics a Compose assertion asks for. That is the difference from
 * the per-component accessibility tests.
 */
@HiltAndroidTest
@UninstallModules(RemoteSourceModule::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = ACCESSIBILITY_CHECKS_MIN_SDK)
class A11yFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val freshInstall = FreshInstall()

    // The Activity must not launch until the graph is bound, and createAndroidComposeRule starts
    // it while the rule evaluates, which is before @Before runs.
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
        remote.alwaysAnswerMovieDetail(
            NetworkResult.Success(MovieFixture.detail(movie = trendingMovies[DETAIL_ROW_INDEX])),
        )
        // Enabling the checks here also runs them ahead of every tap the tests below make.
        composeRule.enableAccessibilityChecks(
            AccessibilityValidator()
                .setRunChecksFromRootView(true)
                .setThrowExceptionFor(AccessibilityCheckResultType.INFO)
                // Suppress the contrast check for images. Posters rarely load in these tests.
                .setSuppressingResultMatcher(
                    matchesChecks(equalTo(ImageContrastCheck::class.java)),
                ),
        )
    }

    @After
    fun tearDown() {
        launcher.close()
    }

    @Test
    fun theTrendingScreenPassesAccessibilityChecks() {
        launcher.launch()
        composeRule.awaitText(trendingMovies.first().title)

        assertScreenIsAccessible()
    }

    @Test
    fun theMovieDetailScreenPassesAccessibilityChecks() {
        val openedTitle = trendingMovies[DETAIL_ROW_INDEX].title

        launcher.launch()
        composeRule.awaitText(trendingMovies.first().title)

        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText(openedTitle))
        composeRule.clickWhenStill(hasText(openedTitle))

        composeRule.awaitText(string(R.string.feature_detail_fact_runtime))

        assertScreenIsAccessible()
    }

    /** Runs the checks over everything on screen, and fails the test with what they report. */
    private fun assertScreenIsAccessible() {
        composeRule.waitForIdle()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
