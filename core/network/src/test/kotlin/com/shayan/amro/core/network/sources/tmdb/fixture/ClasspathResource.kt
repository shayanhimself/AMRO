package com.shayan.amro.core.network.sources.tmdb.fixture

/**
 * Reads one file packaged with this module.
 *
 * It goes through the classpath rather than an Android context, so it works in a plain JVM test
 * with no Robolectric.
 *
 * @param path the resource's path, as in `fixtures/tmdb/trending/page-1.json`.
 * @return the file's contents, or null when nothing is packaged at that path.
 */
internal fun classpathResource(path: String): String? =
    object {}
        .javaClass.classLoader
        ?.getResourceAsStream(path)
        ?.use { it.readBytes().decodeToString() }
