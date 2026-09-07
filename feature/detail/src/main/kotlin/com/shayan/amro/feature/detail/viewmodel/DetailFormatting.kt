package com.shayan.amro.feature.detail.viewmodel

import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.detail.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import kotlin.time.Duration

/**
 * The sources report movie budgets and revenue in this currency.
 */
private const val REPORTED_CURRENCY = "USD"

/** How many decimals a rating reads to. */
private const val RATING_DECIMALS = 1

/** What a running time is split into before it is written as hours and minutes. */
private const val MINUTES_PER_HOUR = 60

/**
 * The default locale every number and date below is written in.
 */
private fun formattingLocale(): Locale = Locale.getDefault(Locale.Category.FORMAT)

/**
 * A running time, as hours and minutes.
 *
 * Example: "2h 37m" for a feature, and "39m" for a short.
 */
internal fun Duration.toRuntimeText(): AmroText {
    val total = inWholeMinutes.toInt()
    val hours = total / MINUTES_PER_HOUR
    val minutes = total % MINUTES_PER_HOUR
    return if (hours == 0) {
        AmroText.Resource(R.string.feature_detail_runtime_minutes, minutes)
    } else {
        AmroText.Resource(R.string.feature_detail_runtime_hours_minutes, hours, minutes)
    }
}

/** A release date, in the long form the reader's locale writes.
 *
 * Example: "March 15, 2026"
 */
internal fun LocalDate.toDateText(): AmroText =
    AmroText.Raw(
        text = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(formattingLocale())
            .format(toJavaLocalDate()),
    )

/**
 * An amount of money, grouped the way the reader's locale groups digits.
 *
 * Example: "$2,332,821,029"
 *
 * @param amount the sum, in the currency the sources report.
 */
internal fun moneyText(amount: Long): AmroText {
    val format = NumberFormat.getCurrencyInstance(formattingLocale())
    format.currency = Currency.getInstance(REPORTED_CURRENCY)
    // Box office is reported in whole units, so the cents a currency format adds say nothing.
    format.maximumFractionDigits = 0
    return AmroText.Raw(format.format(amount))
}

/**
 * A rating, to one decimal.
 *
 * Example: "8.6"
 *
 * @param average the rating, on a scale of ten.
 */
internal fun ratingText(average: Double): AmroText {
    val format = NumberFormat.getInstance(formattingLocale())
    format.minimumFractionDigits = RATING_DECIMALS
    format.maximumFractionDigits = RATING_DECIMALS
    return AmroText.Raw(format.format(average))
}

/**
 * How many votes produced a rating.
 *
 * Example: "1,234"
 *
 * @param count how many votes, which picks the plural form and, grouped, fills it.
 */
internal fun voteCountText(count: Int): AmroText =
    AmroText.Plural(
        id = R.plurals.feature_detail_vote_count,
        count = count,
        NumberFormat.getInstance(formattingLocale()).format(count),
    )
