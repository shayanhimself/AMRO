package com.shayan.amro.core.network.sources.tmdb.mapper

import com.shayan.amro.core.model.ReleaseStatus

/** TMDB's release statuses paired with the app status each one becomes. */
internal val STATUSES_BY_TMDB_NAME =
    mapOf(
        "Rumored" to ReleaseStatus.RUMORED,
        "Planned" to ReleaseStatus.PLANNED,
        "In Production" to ReleaseStatus.IN_PRODUCTION,
        "Post Production" to ReleaseStatus.POST_PRODUCTION,
        "Released" to ReleaseStatus.RELEASED,
        "Canceled" to ReleaseStatus.CANCELED,
    )
