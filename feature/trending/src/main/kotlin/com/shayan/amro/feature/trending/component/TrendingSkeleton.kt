package com.shayan.amro.feature.trending.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.R as CoreUiR

/** How many placeholder rows a first load draws. */
private const val SKELETON_ROW_COUNT = 8

/** A first load, drawn as the rows it is about to become. */
@Composable
internal fun TrendingSkeleton(modifier: Modifier = Modifier) {
    val loading = stringResource(CoreUiR.string.core_ui_loading)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .semantics(mergeDescendants = true) { contentDescription = loading },
    ) {
        repeat(SKELETON_ROW_COUNT) { index -> MovieRowSkeleton(index = index) }
    }
}

@Preview
@Composable
private fun TrendingSkeletonPreview() {
    AmroTheme {
        Surface {
            TrendingSkeleton()
        }
    }
}
