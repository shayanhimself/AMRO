package com.shayan.amro.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.shayan.amro.feature.detail.MovieDetailRoute
import com.shayan.amro.feature.trending.TrendingRoute

/**
 * Maps every key to its route and lets the adaptive scene arrange them.
 *
 * @param backStack owned by the caller, and read here only to render it.
 * @param navigator the owner of every backstack mutation.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AmroNavDisplay(
    backStack: NavBackStack<NavKey>,
    navigator: AmroNavigator,
    modifier: Modifier = Modifier,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive =
        remember(windowAdaptiveInfo) {
            // The default splits into panes at expanded width and nowhere below it. Overridden only
            // so there is no horizontal gap between the panes.
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    val twoPane = directive.maxHorizontalPartitions > 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { navigator.back() },
        sceneStrategies = listOf(listDetailStrategy),
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                // The ViewModel store decorator is what scopes a ViewModel to its entry, so a
                // different MovieDetailKey gets a different view model.
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<TrendingKey>(metadata = ListDetailSceneStrategy.listPane()) {
                    TrendingRoute(onMovieClick = navigator::openMovie)
                }
                entry<MovieDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    MovieDetailRoute(
                        movieId = key.id,
                        // Two panes are shown at once, so there is nowhere for a back affordance
                        // inside the detail pane to go.
                        onBack = if (twoPane) null else navigator::back,
                    )
                }
            },
    )
}
