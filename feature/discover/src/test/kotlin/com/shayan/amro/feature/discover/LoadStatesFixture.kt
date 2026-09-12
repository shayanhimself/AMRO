package com.shayan.amro.feature.discover

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates

/** A read that has nothing further to do. */
private val SETTLED = LoadState.NotLoading(endOfPaginationReached = true)

/**
 * What a list reports about its reads while the head of it is doing [refresh].
 *
 * @param refresh what the read of the head of the list is doing.
 * @param servedEverything whether the source has said it has nothing further to serve, which is
 * what tells a list that is empty from one whose movies have not reached it yet.
 */
internal fun listLoadStates(
    refresh: LoadState,
    servedEverything: Boolean = true,
): CombinedLoadStates {
    val append = LoadState.NotLoading(endOfPaginationReached = servedEverything)
    val states = LoadStates(refresh = refresh, prepend = SETTLED, append = append)
    return CombinedLoadStates(
        refresh = refresh,
        prepend = SETTLED,
        append = append,
        source = states,
        mediator = states,
    )
}
