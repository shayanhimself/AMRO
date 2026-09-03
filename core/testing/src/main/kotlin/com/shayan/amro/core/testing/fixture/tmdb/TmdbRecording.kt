package com.shayan.amro.core.testing.fixture.tmdb

/** Where the recordings sit in the source tree, which is what a reader opening this file wants. */
private const val TMDB_FIXTURE_DIRECTORY = "core/testing/src/main/resources/fixtures/tmdb/"

/** The same directory as the classpath sees it, which is what reads them at run time. */
private const val TMDB_FIXTURE_ROOT = "fixtures/tmdb/"

/** What re-records them. */
private const val RECORDER = "scripts/record-fixtures.py"

/**
 * One TMDB response, saved to a file so a test can read it back.
 *
 * The file holds what TMDB really sent, so a test reading it checks the app against the provider.
 *
 * @property path where the file sits under the fixture root.
 */
interface TmdbRecording {
    val path: String

    /**
     * Reads the json file from resources of this module.
     *
     * @throws IllegalStateException naming the file that is missing, which is a recording renamed
     * or deleted rather than anything a provider did.
     */
    val json: String
        get() =
            classpathResource(TMDB_FIXTURE_ROOT + path)
                ?: error("no recording at $TMDB_FIXTURE_DIRECTORY$path. Re-record with $RECORDER")
}

/**
 * This recording, read into the wire shape [T] the way the client reads it.
 *
 * The shape is resolved where this is called, so a module's own wire types stay internal to it.
 */
inline fun <reified T> TmdbRecording.decode(): T = testJson.decodeFromString(json)
