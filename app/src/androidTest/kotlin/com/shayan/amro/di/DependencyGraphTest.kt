package com.shayan.amro.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.network.sources.tmdb.TmdbConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val BASE_URL_SUFFIX = "/"

/**
 * Every binding the app resolves at startup, injected on a real device.
 *
 * The JVM tests build their subjects by hand, so a module that fails to provide something is not
 * caught until launch. Injecting here is what proves the graph the app actually runs on.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DependencyGraphTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var tmdbConfig: TmdbConfig

    @Test
    fun everyStartupBindingResolves() {
        hiltRule.inject()

        assertNotNull(tmdbConfig)
    }

    // The token is a build input that defaults to empty, so its value is not asserted on. The
    // address is: every endpoint is resolved against it, and one that does not end in a slash
    // resolves them onto the wrong path.
    @Test
    fun theConfiguredBaseUrlIsARoot() {
        hiltRule.inject()

        assertTrue(tmdbConfig.baseUrl.endsWith(BASE_URL_SUFFIX))
    }
}
