package com.shayan.amro.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.data.MovieDetailRepository
import com.shayan.amro.core.data.TrendingMoviesRepository
import com.shayan.amro.core.network.MovieRemoteDataSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertNotNull

/**
 * Every binding the app resolves at startup, injected on a real device.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DependencyGraphTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var movieRemoteDataSource: MovieRemoteDataSource

    @Inject
    lateinit var trendingMoviesRepository: TrendingMoviesRepository

    @Inject
    lateinit var movieDetailRepository: MovieDetailRepository

    @Test
    fun everyStartupBindingResolves() {
        hiltRule.inject()

        assertNotNull(movieRemoteDataSource)
        assertNotNull(trendingMoviesRepository)
        assertNotNull(movieDetailRepository)
    }
}
