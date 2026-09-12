package com.shayan.amro.feature.discover

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.shayan.amro.core.testing.preview.FontScalePreviews
import com.shayan.amro.core.testing.preview.FormFactorPreviews
import com.shayan.amro.core.testing.preview.ThemePreviews
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.discover.ui.DiscoverPreviewData
import com.shayan.amro.feature.discover.ui.DiscoverScreenPreviewHost
import com.shayan.amro.feature.discover.ui.FilterSheetPreviewHost

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverSkeletonPreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.SKELETON) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverLoadedPreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.LOADED) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverSelectedPreview() {
    AmroTheme {
        DiscoverScreenPreviewHost(
            state = DiscoverPreviewData.LOADED,
            selectedMovieId = DiscoverPreviewData.ROWS.first().movieId,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverNarrowedPreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.NARROWED) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverWithNoticePreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.WITH_NOTICE) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverEmptyFromFilterPreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.EMPTY_FROM_FILTER) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverErrorPreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.ERROR) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun DiscoverFilterSheetPreview() {
    AmroTheme { FilterSheetPreviewHost() }
}

/**
 * The loaded state is the densest one and the only place a reflow or a clip has room to show, so
 * the width and the text-size crosses are taken there and nowhere else.
 */
@PreviewTest
@FormFactorPreviews
@Composable
private fun DiscoverLoadedFormFactorPreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.LOADED) }
}

@PreviewTest
@FontScalePreviews
@Composable
private fun DiscoverLoadedFontScalePreview() {
    AmroTheme { DiscoverScreenPreviewHost(state = DiscoverPreviewData.LOADED) }
}
