package com.shayan.amro.core.network.sources.tmdb.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A recorded poster, which TMDB serves vertical. */
private const val POSTER_PATH = "/yihdXomYb5kTeSivtFndMy5iDmf.jpg"

/** A recorded backdrop of the same movie, served horizontal */
private const val BACKDROP_PATH = "/8Tfys3mDZVp4tNoH2ktm06a0Tau.jpg"

private const val IMAGE_HOST = "https://image.tmdb.org/t/p/"

private const val SMALL_POSTER_URL = IMAGE_HOST + "w185" + POSTER_PATH
private const val LARGE_POSTER_URL = IMAGE_HOST + "w500" + POSTER_PATH
private const val SMALL_BACKDROP_URL = IMAGE_HOST + "w300" + BACKDROP_PATH
private const val LARGE_BACKDROP_URL = IMAGE_HOST + "w1280" + BACKDROP_PATH

private const val BLANK_PATH = "   "

class TmdbImageMapperTest {
    @Test
    fun `a poster path fills both sizes with the widths TMDB publishes`() {
        val poster = posterRef(POSTER_PATH)

        assertEquals(SMALL_POSTER_URL, poster?.small)
        assertEquals(LARGE_POSTER_URL, poster?.large)
    }

    @Test
    fun `a backdrop path fills both sizes with its own widths`() {
        val backdrop = backdropRef(BACKDROP_PATH)

        assertEquals(SMALL_BACKDROP_URL, backdrop?.small)
        assertEquals(LARGE_BACKDROP_URL, backdrop?.large)
    }

    @Test
    fun `an absent path yields no image`() {
        assertNull(posterRef(null))
        assertNull(backdropRef(null))
    }

    @Test
    fun `a blank path yields no image`() {
        assertNull(posterRef(BLANK_PATH))
        assertNull(backdropRef(BLANK_PATH))
    }
}
