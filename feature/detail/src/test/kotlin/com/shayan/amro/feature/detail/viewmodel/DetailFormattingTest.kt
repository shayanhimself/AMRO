package com.shayan.amro.feature.detail.viewmodel

import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.detail.R
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/** A running time over an hour, and one under it. */
private const val FEATURE_LENGTH_MINUTES = 157
private const val SHORT_MINUTES = 39

private const val TEN_DIGIT_AMOUNT = 2_332_821_029L

/** A rating with more decimals than the screen reads it to. */
private const val UNROUNDED_RATING = 8.646

private const val MANY_VOTES = 7472
private const val ONE_VOTE = 1

class DetailFormattingTest {
    private lateinit var original: Locale

    @Before
    fun pinLocale() {
        original = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `a running time over an hour is named as hours and minutes`() {
        assertEquals(
            AmroText.Resource(R.string.feature_detail_runtime_hours_minutes, 2, 37),
            FEATURE_LENGTH_MINUTES.minutes.toRuntimeText(),
        )
    }

    @Test
    fun `a running time under an hour is named as minutes alone`() {
        assertEquals(
            AmroText.Resource(R.string.feature_detail_runtime_minutes, SHORT_MINUTES),
            SHORT_MINUTES.minutes.toRuntimeText(),
        )
    }

    @Test
    fun `an amount at ten digits reads grouped, in the currency the sources report`() {
        assertEquals(AmroText.Raw("$2,332,821,029"), moneyText(TEN_DIGIT_AMOUNT))
    }

    @Test
    fun `a rating reads to one decimal`() {
        assertEquals(AmroText.Raw("8.6"), ratingText(UNROUNDED_RATING))
    }

    @Test
    fun `a release date reads in the long form`() {
        assertEquals(
            AmroText.Raw("March 15, 2026"),
            LocalDate.parse("2026-03-15").toDateText(),
        )
    }

    @Test
    fun `a vote count is grouped, and picks its form by the count itself`() {
        assertEquals(
            AmroText.Plural(R.plurals.feature_detail_vote_count, MANY_VOTES, "7,472"),
            voteCountText(MANY_VOTES),
        )
        assertEquals(
            AmroText.Plural(R.plurals.feature_detail_vote_count, ONE_VOTE, "1"),
            voteCountText(ONE_VOTE),
        )
    }
}
