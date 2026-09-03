package com.shayan.amro.core.testing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What every recorded body starts with, since TMDB answers with a JSON object throughout. */
private const val JSON_OBJECT_OPENING = "{"

class TmdbFixtureTest {
    @Test
    fun `every named fixture is packaged and holds a JSON object`() {
        val empty =
            TmdbFixture.entries.filterNot { fixture ->
                fixture.json.trimStart().startsWith(JSON_OBJECT_OPENING)
            }

        assertEquals(emptyList(), empty, "fixtures that are missing or are not a JSON object")
    }

    @Test
    fun `the trending pages are listed in the order they were served`() {
        assertEquals(TmdbFixture.TRENDING_PAGE_1, TmdbFixture.TRENDING_PAGES.first())
        assertTrue(
            TmdbFixture.TRENDING_PAGES.zipWithNext().all { (earlier, later) ->
                earlier.ordinal < later.ordinal
            },
            "the trending pages are out of order",
        )
    }
}
