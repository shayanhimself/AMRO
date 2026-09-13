package com.shayan.amro.feature.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayan.amro.core.data.MovieDetailRepository
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.ui.viewmodel.SUBSCRIPTION_TIMEOUT_MILLIS
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = MovieDetailViewModel.Factory::class)
internal class MovieDetailViewModel
    @AssistedInject
    constructor(
        @Assisted private val movieId: String,
        private val movieDetailRepository: MovieDetailRepository,
    ) : ViewModel() {
        private val error = MutableStateFlow<DataError?>(null)

        /** Whether the launch fetch has run for this view model. */
        private var hasFetchedOnLaunch = false

        val uiState: StateFlow<MovieDetailUiState> =
            combine(
                movieDetailRepository.getMovieFlow(movieId),
                movieDetailRepository.getMovieDetailFlow(movieId),
                error,
            ) { movie, detail, error ->
                toMovieDetailUiState(movie = movie, detail = detail, error = error)
            }.onStart { fetchOnLaunch() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                    initialValue = MovieDetailUiState(),
                )

        /** Fetches the record again, and reports what that attempt did. */
        fun onRetry() {
            viewModelScope.launch {
                // The cause describes the attempt that failed, so it stops being true the moment
                // another one starts. Clearing it first is what returns Retry to the skeleton.
                error.value = null
                error.value = movieDetailRepository.refresh(movieId)
            }
        }

        /**
         * Fetches the movie detail the first time it's called, and not again.
         */
        private fun fetchOnLaunch() {
            if (hasFetchedOnLaunch) return
            hasFetchedOnLaunch = true
            onRetry()
        }

        /** Builds the view model for one movie, which the route names as it enters composition. */
        @AssistedFactory
        interface Factory {
            fun create(movieId: String): MovieDetailViewModel
        }
    }
