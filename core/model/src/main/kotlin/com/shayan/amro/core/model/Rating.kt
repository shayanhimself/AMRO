package com.shayan.amro.core.model

/**
 * An audience score, and how many votes produced it.
 *
 * A movie with no score at all carries no `Rating`.
 *
 * @property average the score the app owns, in scale of ten. A source scoring on another
 * scale normalises at its own mapper.
 * @property count how many votes. null for a source that reports no vote count.
 */
data class Rating(
    val average: Double,
    val count: Int?,
)
