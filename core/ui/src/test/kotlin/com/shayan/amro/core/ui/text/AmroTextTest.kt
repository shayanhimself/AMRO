package com.shayan.amro.core.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.R
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** A film the poster description is formatted with. */
private const val FILM = "The Mongoose"

/** What sits between two joined texts. */
private const val SEPARATOR = ", "

@RunWith(AndroidJUnit4::class)
class AmroTextTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `raw text resolves to itself`() {
        assertEquals(FILM, AmroText.Raw(FILM).resolve(context))
    }

    @Test
    fun `a resource resolves to its copy`() {
        assertEquals(
            string(R.string.core_ui_retry),
            AmroText.Resource(R.string.core_ui_retry).resolve(context),
        )
    }

    @Test
    fun `a resource fills its placeholders from the arguments`() {
        assertEquals(
            string(R.string.core_ui_poster_description, FILM),
            AmroText.Resource(R.string.core_ui_poster_description, FILM).resolve(context),
        )
    }

    @Test
    fun `an argument that is itself a text resolves first`() {
        val nested = AmroText.Resource(R.string.core_ui_genre_thriller)

        assertEquals(
            string(R.string.core_ui_poster_description, string(R.string.core_ui_genre_thriller)),
            AmroText.Resource(R.string.core_ui_poster_description, nested).resolve(context),
        )
    }

    @Test
    fun `joined texts read as one, separated`() {
        val joined =
            AmroText.Joined(
                texts =
                    persistentListOf(
                        AmroText.Resource(R.string.core_ui_genre_action),
                        AmroText.Raw(FILM),
                    ),
                separator = SEPARATOR,
            )

        assertEquals(
            string(R.string.core_ui_genre_action) + SEPARATOR + FILM,
            joined.resolve(context),
        )
    }

    @Test
    fun `joined texts run together where no separator is given`() {
        val joined = AmroText.Joined(persistentListOf(AmroText.Raw(FILM), AmroText.Raw(FILM)))

        assertEquals(FILM + FILM, joined.resolve(context))
    }

    @Test
    fun `nothing to say resolves to nothing`() {
        assertEquals("", AmroText.Empty.resolve(context))
    }
}
