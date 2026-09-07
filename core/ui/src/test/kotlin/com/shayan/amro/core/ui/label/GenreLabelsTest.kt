package com.shayan.amro.core.ui.label

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.testing.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenreLabelsTest {
    @Test
    fun `every genre is called something`() {
        Genre.entries.forEach { genre ->
            assertTrue(genre.name, string(genre.labelRes).isNotBlank())
        }
    }

    @Test
    fun `no two genres are called the same thing`() {
        val labels = Genre.entries.map { string(it.labelRes) }

        assertEquals(labels.size, labels.toSet().size)
    }
}
