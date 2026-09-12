package com.shayan.amro.feature.discover.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.shayan.amro.core.data.MoviesRepository
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.ui.viewmodel.SUBSCRIPTION_TIMEOUT_MILLIS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Where the genre selection is saved, as the enum's own names. */
private const val KEY_GENRES = "discover.genres"

/** Where the ordering key is saved, as the enum's own name. */
private const val KEY_SORT_KEY = "discover.sortKey"

/** Where the ordering direction is saved, as the enum's own name. */
private const val KEY_SORT_DIRECTION = "discover.sortDirection"

@HiltViewModel
internal class DiscoverViewModel
    @Inject
    constructor(
        moviesRepository: MoviesRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val genreNames =
            savedStateHandle.getMutableStateFlow(KEY_GENRES, ArrayList<String>())
        private val sortKeyName =
            savedStateHandle.getMutableStateFlow(KEY_SORT_KEY, MovieSort.DEFAULT.key.name)
        private val sortDirectionName =
            savedStateHandle.getMutableStateFlow(
                KEY_SORT_DIRECTION,
                MovieSort.DEFAULT.direction.name,
            )

        private val selection: StateFlow<MovieSelection> =
            combine(genreNames, sortKeyName, sortDirectionName) { names, key, direction ->
                MovieSelection(
                    genres = names.mapTo(mutableSetOf(), Genre::valueOf),
                    sort =
                        MovieSort(
                            key = SortKey.valueOf(key),
                            direction = SortDirection.valueOf(direction),
                        ),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = MovieSelection(),
            )

        val uiState: StateFlow<DiscoverUiState> =
            selection
                .map { it.toDiscoverUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                    initialValue = MovieSelection().toDiscoverUiState(),
                )

        /**
         * The rows the current selection produces.
         *
         * `cachedIn` sits after the swap rather than inside it, so a chip tap re-queries the
         * database rather than the network when that selection is already cached.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val movieRows: Flow<PagingData<MovieRowUiState>> =
            selection
                .flatMapLatest { moviesRepository.moviesPagingFlow(it) }
                .map { rows -> rows.map { it.toRow() } }
                .cachedIn(viewModelScope)

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
            sortKeyName.value = key.name
        }

        /**
         * Runs the ordering in [direction], keeping the key.
         */
        fun onSelectSortDirection(direction: SortDirection) {
            sortDirectionName.value = direction.name
        }

        /**
         * Drops every selected genre, which resolves the empty-from-filter state.
         */
        fun onClearGenres() {
            genreNames.value = ArrayList()
        }

        /** Drops the genre selection and returns the order to the default. */
        fun onReset() {
            onClearGenres()
            sortKeyName.value = MovieSort.DEFAULT.key.name
            sortDirectionName.value = MovieSort.DEFAULT.direction.name
        }
    }
