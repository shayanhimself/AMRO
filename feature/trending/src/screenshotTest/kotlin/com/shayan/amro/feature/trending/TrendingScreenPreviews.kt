package com.shayan.amro.feature.trending

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.shayan.amro.core.testing.preview.FontScalePreviews
import com.shayan.amro.core.testing.preview.FormFactorPreviews
import com.shayan.amro.core.testing.preview.ThemePreviews
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.trending.ui.FilterSheetPreviewHost
import com.shayan.amro.feature.trending.ui.TrendingPreviewData
import com.shayan.amro.feature.trending.ui.TrendingScreenPreviewHost

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingSkeletonPreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.SKELETON) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingLoadedPreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.LOADED) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingSelectedPreview() {
    AmroTheme {
        TrendingScreenPreviewHost(
            state = TrendingPreviewData.LOADED,
            selectedMovieId = TrendingPreviewData.ROWS.first().movieId,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingNarrowedPreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.NARROWED) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingWithNoticePreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.WITH_NOTICE) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingEmptyFromFilterPreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.EMPTY_FROM_FILTER) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingErrorPreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.ERROR) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun TrendingFilterSheetPreview() {
    AmroTheme { FilterSheetPreviewHost() }
}

/**
 * The loaded state is the densest one and the only place a reflow or a clip has room to show, so
 * the width and the text-size crosses are taken there and nowhere else.
 */
@PreviewTest
@FormFactorPreviews
@Composable
private fun TrendingLoadedFormFactorPreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.LOADED) }
}

@PreviewTest
@FontScalePreviews
@Composable
private fun TrendingLoadedFontScalePreview() {
    AmroTheme { TrendingScreenPreviewHost(state = TrendingPreviewData.LOADED) }
}
