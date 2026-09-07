package com.shayan.amro.feature.trending.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayan.amro.core.data.TrendingMoviesRepository
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.ui.viewmodel.SUBSCRIPTION_TIMEOUT_MILLIS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the genre selection is saved, as the enum's own names. */
private const val KEY_GENRES = "trending.genres"

/** Where the ordering is saved. */
private const val KEY_SORT = "trending.sort"

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

@HiltViewModel
internal class TrendingViewModel
    @Inject
    constructor(
        private val trendingMoviesRepository: TrendingMoviesRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val genreNames =
            savedStateHandle.getMutableStateFlow(KEY_GENRES, ArrayList<String>())
        private val sort = savedStateHandle.getMutableStateFlow(KEY_SORT, MovieSort.DEFAULT)
        private val refreshState = MutableStateFlow(RefreshState())

        /** Whether the launch refresh has run for this view model. */
        private var hasRefreshedOnLaunch = false

        val uiState: StateFlow<TrendingUiState> =
            combine(
                trendingMoviesRepository.getTrendingMoviesFlow(),
                genreNames,
                sort,
                refreshState,
            ) { movies, names, sort, refresh ->
                movies.toTrendingUiState(selectedGenreNames = names, sort = sort, refresh = refresh)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = TrendingUiState(),
            )

        /**
         * Fetches the movies the first time the screen is shown, and not again.
         */
        fun onLaunch() {
            if (hasRefreshedOnLaunch) return
            hasRefreshedOnLaunch = true
            onRefresh()
        }

        /** Fetches the trending set again, and reports what that attempt did. */
        fun onRefresh() {
            viewModelScope.launch {
                refreshState.value = RefreshState(isRefreshing = true, error = null)
                val error = trendingMoviesRepository.refresh()
                refreshState.value = RefreshState(isRefreshing = false, error = error)
            }
        }

        /**
         * Adds [genre] to the selection, or removes it when it is already selected.
         */
        fun onToggleGenre(genre: String) {
            genreNames.update { genres ->
                ArrayList(if (genre in genres) genres - genre else genres + genre)
            }
        }

        /**
         * Orders the list by [key], keeping the direction.
         */
        fun onSelectSortKey(key: SortKey) {
            sort.update { it.copy(key = key) }
        }

        /**
         * Runs the ordering in [direction], keeping the key.
         */
        fun onSelectSortDirection(direction: SortDirection) {
            sort.update { it.copy(direction = direction) }
        }

        /**
         * Drops every selected genre, resolves the empty-from-filter state.
         */
        fun onClearGenres() {
            genreNames.value = ArrayList()
        }

        /** Drops the genre selection and returns the order to the default. */
        fun onReset() {
            onClearGenres()
            sort.value = MovieSort.DEFAULT
        }
    }
