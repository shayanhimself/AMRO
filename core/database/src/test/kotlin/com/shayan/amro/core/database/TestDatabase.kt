package com.shayan.amro.core.database

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * An empty in-memory database on the test scheduler.
 *
 * The bundled driver, so a test runs the SQLite the app ships rather than the one Robolectric
 * supplies.
 */
internal fun TestScope.testDatabase(): AmroDatabase =
    Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AmroDatabase::class.java,
        ).setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(StandardTestDispatcher(testScheduler))
        .build()
