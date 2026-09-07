package com.shayan.amro.core.network.sources.tmdb.fixture

import kotlin.test.Test
import kotlin.test.assertEquals

/** What every recorded body starts with, since TMDB answers with a JSON object throughout. */
private const val JSON_OBJECT_OPENING = "{"

class TmdbFixtureTest {
    @Test
    fun `every named fixture is packaged and holds a JSON object`() {
        val empty =
            TmdbFixture.ALL.filterNot { fixture ->
                fixture.json.trimStart().startsWith(JSON_OBJECT_OPENING)
            }

        assertEquals(emptyList(), empty, "fixtures that are missing or are not a JSON object")
    }

    @Test
    fun `no two fixtures resolve to the same recording`() {
        val shared =
            TmdbFixture.ALL
                .groupBy { it.path }
                .filterValues { it.size > 1 }
                .keys

        assertEquals(emptySet(), shared, "recordings two fixtures both point at")
    }
}
