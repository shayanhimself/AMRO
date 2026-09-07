package com.shayan.amro.feature.detail

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.shayan.amro.core.testing.preview.FontScalePreviews
import com.shayan.amro.core.testing.preview.FormFactorPreviews
import com.shayan.amro.core.testing.preview.ThemePreviews
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.detail.ui.MovieDetailPreviewData
import com.shayan.amro.feature.detail.ui.MovieDetailScreenPreviewHost

@PreviewTest
@ThemePreviews
@Composable
private fun MovieDetailSkeletonPreview() {
    AmroTheme { MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.SKELETON) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun MovieDetailSkeletonWithoutSummaryPreview() {
    AmroTheme {
        MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.SKELETON_WITHOUT_SUMMARY)
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun MovieDetailLoadedPreview() {
    AmroTheme { MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.LOADED) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun MovieDetailMissingDataPreview() {
    AmroTheme { MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.MISSING_DATA) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun MovieDetailErrorPreview() {
    AmroTheme { MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.ERROR) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun MovieDetailCrowdedPreview() {
    AmroTheme { MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.CROWDED_RECORD) }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun MovieDetailCrowdedFormFactorPreview() {
    AmroTheme { MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.CROWDED_RECORD) }
}

@PreviewTest
@FontScalePreviews
@Composable
private fun MovieDetailCrowdedFontScalePreview() {
    AmroTheme { MovieDetailScreenPreviewHost(state = MovieDetailPreviewData.CROWDED_RECORD) }
}
