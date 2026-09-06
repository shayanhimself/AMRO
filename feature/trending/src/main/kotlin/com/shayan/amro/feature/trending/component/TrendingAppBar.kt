package com.shayan.amro.feature.trending.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.designsystem.component.BadgeTone
import com.shayan.amro.core.ui.designsystem.component.DsBadge
import com.shayan.amro.core.ui.designsystem.component.DsIconButton
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.trending.R

/** How far the badge is pulled onto the filter action it counts for. */
private val BADGE_INSET = 6.dp

/**
 * The screen's name, and the action that opens the sheet.
 *
 * @param activeSelectionCount how many genres are selected, plus one for a sort away from the
 * default, which is what the badge states.
 * @param onFilterClick opens the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrendingAppBar(
    activeSelectionCount: Int,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.feature_trending_title)) },
        actions = {
            Box {
                DsIconButton(
                    glyph = Glyphs.TUNE,
                    contentDescription =
                        if (activeSelectionCount == 0) {
                            stringResource(R.string.feature_trending_filter_action)
                        } else {
                            pluralStringResource(
                                R.plurals.feature_trending_filter_action_active,
                                activeSelectionCount,
                                activeSelectionCount,
                            )
                        },
                    onClick = onFilterClick,
                )
                if (activeSelectionCount > 0) {
                    DsBadge(
                        tone = BadgeTone.Primary,
                        text = activeSelectionCount.toString(),
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = BADGE_INSET, end = BADGE_INSET),
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun TrendingAppBarPreview() {
    AmroTheme {
        TrendingAppBar(activeSelectionCount = 0, onFilterClick = {})
    }
}

@Preview
@Composable
private fun TrendingAppBarWithActiveCountPreview() {
    AmroTheme {
        TrendingAppBar(activeSelectionCount = 3, onFilterClick = {})
    }
}
