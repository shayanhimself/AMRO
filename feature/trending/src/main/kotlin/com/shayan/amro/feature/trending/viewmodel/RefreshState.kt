package com.shayan.amro.feature.trending.viewmodel

import com.shayan.amro.core.model.DataError

/**
 * The state of the last refresh.
 *
 * @property isRefreshing whether one is running now.
 * @property error the cause the last one failed with, or null.
 */
internal data class RefreshState(
    val isRefreshing: Boolean = false,
    val error: DataError? = null,
)
