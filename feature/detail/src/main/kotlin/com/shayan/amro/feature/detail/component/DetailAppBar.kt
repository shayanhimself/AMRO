package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.component.DsIconButton
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.detail.R

private const val BACK_BUTTON_ALPHA_MULTIPLIER = 0.42f

/**
 * The detail screen's AppBar.
 *
 * @param title what the bar names the screen.
 * @param collapsedFraction how far the header has scrolled under the bar, from 0 at rest to 1 once
 * it is past. It arrives as a function so the scroll position is read here, which keeps a scroll
 * from recomposing the screen that hosts the bar.
 * @param onBack leaves the screen, null where there is nowhere to go back to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailAppBar(
    title: String?,
    collapsedFraction: () -> Float,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val collapsed = collapsedFraction()
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(modifier = modifier) {
        TopAppBar(
            title = {
                if (title != null && collapsed > 0f) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(collapsed),
                    )
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    Box(
                        modifier =
                            Modifier.background(
                                color = fadingBackground(collapsed),
                                shape = CircleShape,
                            ),
                    ) {
                        DsIconButton(
                            glyph = Glyphs.ARROW_BACK,
                            contentDescription = stringResource(R.string.feature_detail_back),
                            onClick = onBack,
                        )
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = collapsed),
                    scrolledContainerColor = Color.Transparent,
                ),
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomStart),
            color = outline.copy(alpha = collapsed),
        )
    }
}

/**
 * The background behind the back button, which is more prominent when the header is at rest and
 * less so when the header has scrolled under the bar.
 *
 * @param collapsedFraction how far the header has scrolled under the bar, from 0 at rest to 1 once
 * it is past.
 */
@Composable
private fun fadingBackground(collapsedFraction: Float): Color =
    MaterialTheme.colorScheme.surfaceContainer.copy(
        alpha = (1f - collapsedFraction) * BACK_BUTTON_ALPHA_MULTIPLIER,
    )

@Preview
@Composable
private fun DetailAppBarAtRestPreview() {
    AmroTheme {
        DetailAppBar(title = "Project Hail Mary", collapsedFraction = { 0f }, onBack = {})
    }
}

@Preview
@Composable
private fun DetailAppBarCollapsedPreview() {
    AmroTheme {
        DetailAppBar(title = "Project Hail Mary", collapsedFraction = { 1f }, onBack = {})
    }
}

@Preview
@Composable
private fun DetailAppBarWithoutBackPreview() {
    AmroTheme {
        DetailAppBar(title = "Project Hail Mary", collapsedFraction = { 1f }, onBack = null)
    }
}
