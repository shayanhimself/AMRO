package com.shayan.amro.helpers

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/**
 * Empties the app's storage before each instrumented test, so every one of them starts as a fresh
 * install.
 * A second run would otherwise read the list the first one cached and pass with the network path
 * dead.
 */
class FreshInstall : ExternalResource() {
    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.databaseList().forEach(context::deleteDatabase)
    }
}
