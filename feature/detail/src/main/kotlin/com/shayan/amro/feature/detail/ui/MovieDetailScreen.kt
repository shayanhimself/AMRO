package com.shayan.amro.feature.detail.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.shayan.amro.core.ui.component.MessagePanel
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.component.DsButton
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.detail.R
import com.shayan.amro.feature.detail.component.BACKDROP_HEIGHT
import com.shayan.amro.feature.detail.component.DetailAppBar
import com.shayan.amro.feature.detail.component.DetailFacts
import com.shayan.amro.feature.detail.component.DetailHeader
import com.shayan.amro.feature.detail.component.DetailSkeleton
import com.shayan.amro.feature.detail.component.GenreChips
import com.shayan.amro.feature.detail.viewmodel.MovieDetailContent
import com.shayan.amro.feature.detail.viewmodel.MovieDetailUiState
import com.shayan.amro.core.ui.R as CoreUiR

/**
 * Shows one movie's detial, in the three states the data can put it in. Stateless.
 *
 * @param uiState what to render.
 * @param onRetry fetches the record again.
 * @param onOpenImdb hands the title's address to whatever opens addresses.
 * @param onBack leaves the screen, null where there is nowhere to go back to.
 */
@Composable
internal fun MovieDetailScreen(
    uiState: MovieDetailUiState,
    onRetry: () -> Unit,
    onOpenImdb: (url: String) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val collapsedFraction =
        rememberCollapsedFraction(
            scrollState = scrollState,
            hasHeader = uiState.content !is MovieDetailContent.Error,
        )

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailAppBar(
                title = uiState.title,
                collapsedFraction = { collapsedFraction.value },
                onBack = onBack,
            )
        },
    ) { contentPadding ->
        when (val content = uiState.content) {
            is MovieDetailContent.Skeleton -> {
                DetailSkeleton(
                    title = uiState.title.orEmpty(),
                    content = content,
                    bottomPadding = contentPadding.calculateBottomPadding(),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                )
            }

            is MovieDetailContent.Loaded -> {
                LoadedBody(
                    title = uiState.title.orEmpty(),
                    content = content,
                    onOpenImdb = onOpenImdb,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                    bottomPadding = contentPadding.calculateBottomPadding(),
                )
            }

            is MovieDetailContent.Error -> {
                MessagePanel(
                    glyph = content.glyph,
                    title = content.title.resolve(),
                    actionLabel = stringResource(CoreUiR.string.core_ui_retry),
                    onAction = onRetry,
                    glyphTint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}

/**
 * The movie art, the overview text, the fact grid and the IMDB action.
 *
 * @param title the film, which the header and the poster's description both read.
 * @param content the movie detail content.
 * @param onOpenImdb hands the title's address to whatever opens addresses.
 * @param bottomPadding what the navigation bar takes off the bottom of the window.
 */
@Composable
private fun LoadedBody(
    title: String,
    content: MovieDetailContent.Loaded,
    onOpenImdb: (url: String) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    // stacks header over body and owns the scroll's bottom padding.
    Column(
        modifier = modifier.padding(bottom = bottomPadding + Spacing.s10),
    ) {
        DetailHeader(
            title = title,
            posterUrl = content.posterUrl,
            backdropUrl = content.backdropUrl,
            badge = content.badge,
        )
        // The body is padded to the sides and between sections, but the header is not.
        Column(
            modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.s5),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            // The title and column are closer together, so they sit in their own column.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                content.tagline?.let { tagline ->
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (content.genreLabels.isNotEmpty()) {
                GenreChips(
                    genreLabels = content.genreLabels,
                    description = content.genreDescription,
                )
            }
            content.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            DetailFacts(facts = content.facts, modifier = Modifier.padding(top = Spacing.s1))
            content.imdbUrl?.let { url ->
                ImdbAction(onClick = { onOpenImdb(url) })
            }
        }
    }
}

/**
 * The row that leaves the app for the title's IMDB page.
 */
@Composable
private fun ImdbAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // content description names the browser, so the app disappearing afterwards is not a surprise.
    val description = stringResource(R.string.feature_detail_imdb_action_description)
    DsButton(
        text = stringResource(R.string.feature_detail_imdb_action),
        onClick = onClick,
        variant = ButtonVariant.Outlined,
        leadingGlyph = Glyphs.OPEN_IN_NEW,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = Spacing.s2)
                .semantics { contentDescription = description },
    )
}

/**
 * How far the header has scrolled under the app bar, from 0 at rest to 1 once it is past.
 *
 * It is a [State] rather than a number so the caller can hand the read to whoever draws with it.
 * Returning the number would record the read against the caller, and every scroll frame would
 * recompose the screen itself too.
 *
 * @param scrollState where the body under the bar has been scrolled to.
 * @param hasHeader whether the screen draws a header for the bar to collapse over.
 */
@Composable
internal fun rememberCollapsedFraction(
    scrollState: ScrollState,
    hasHeader: Boolean,
): State<Float> {
    val backdropHeightPx = with(LocalDensity.current) { BACKDROP_HEIGHT.toPx() }
    return remember(backdropHeightPx, hasHeader) {
        derivedStateOf {
            if (!hasHeader) {
                1f
            } else {
                (scrollState.value / backdropHeightPx).coerceIn(0f, 1f)
            }
        }
    }
}

@Preview
@Composable
private fun MovieDetailScreenPreview() {
    AmroTheme {
        MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.LOADED)
    }
}

@Preview
@Composable
private fun MovieDetailScreenMissingDataPreview() {
    AmroTheme {
        MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.MISSING_DATA)
    }
}

@Preview
@Composable
private fun MovieDetailScreenSkeletonPreview() {
    AmroTheme {
        MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.SKELETON)
    }
}

@Preview
@Composable
private fun MovieDetailScreenErrorPreview() {
    AmroTheme {
        MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.ERROR)
    }
}
