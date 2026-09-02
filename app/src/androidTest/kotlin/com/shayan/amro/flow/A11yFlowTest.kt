package com.shayan.amro.flow

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.shayan.amro.core.network.TmdbConfig
import com.shayan.amro.di.NetworkModule
import com.shayan.amro.wire.LocalTmdb
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The checks read the node tree through a platform API that arrived in Android 14, below which
// the framework has nothing to hand them.
private const val ACCESSIBILITY_CHECKS_MIN_SDK = 34

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
@UninstallModules(NetworkModule::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = ACCESSIBILITY_CHECKS_MIN_SDK)
class A11yFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val tmdb = LocalTmdb()

    // The Activity must not launch until the graph is bound, and createAndroidComposeRule starts
    // it while the rule evaluates, which is before @Before runs.
    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    private val launcher = AppLauncher()

    /** The address the app resolves its endpoints against, pointed at the local server. */
    @BindValue
    @JvmField
    val tmdbConfig: TmdbConfig = tmdb.config

    @Before
    fun setUp() {
        hiltRule.inject()
        // Enabling the checks here also runs them ahead of every tap the tests below make.
        composeRule.enableAccessibilityChecks(
            AccessibilityValidator()
                .setRunChecksFromRootView(true)
                .setThrowExceptionFor(AccessibilityCheckResultType.INFO),
        )
    }

    @After
    fun tearDown() {
        launcher.close()
    }

    // TODO: check the movie detail screen too, once the trending list holds an item to open one
    //  from.
    @Test
    fun theTrendingScreenPassesAccessibilityChecks() {
        launcher.launch()
        composeRule.waitForIdle()

        assertScreenIsAccessible()
    }

    /** Runs the checks over everything on screen, and fails the test with what they report. */
    private fun assertScreenIsAccessible() {
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
