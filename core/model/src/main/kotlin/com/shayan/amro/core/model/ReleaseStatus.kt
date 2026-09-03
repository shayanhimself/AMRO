package com.shayan.amro.core.model

/**
 * Where a movie sits in its production and release cycle.
 *
 * It is owned by the app, and sources map their own statuses to it. A source that reports a status
 * the app does not know maps it to [UNKNOWN].
 */
enum class ReleaseStatus {
    RUMORED,
    PLANNED,
    IN_PRODUCTION,
    POST_PRODUCTION,
    RELEASED,
    CANCELED,
    UNKNOWN,
}
