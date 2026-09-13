package com.shayan.amro.feature.detail.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
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
 * it is past. It arrives as a function so the scroll position is read only where the bar draws,
 * which keeps a scroll from recomposing the bar or the screen that hosts it.
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
    val isTitleShown by remember(collapsedFraction) {
        derivedStateOf { collapsedFraction() > 0f }
    }
    val container = MaterialTheme.colorScheme.surfaceContainer
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier.drawBehind { drawRect(color = container, alpha = collapsedFraction()) },
    ) {
        TopAppBar(
            title = {
                if (title != null && isTitleShown) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = collapsedFraction() },
                    )
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    DsIconButton(
                        glyph = Glyphs.ARROW_BACK,
                        contentDescription = stringResource(R.string.feature_detail_back),
                        onClick = onBack,
                        modifier =
                            Modifier.fadingBackground(
                                color = container,
                                collapsedFraction = collapsedFraction,
                            ),
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    // The color is drawn behind the bar.
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
        )
        HorizontalDivider(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer { alpha = collapsedFraction() },
            color = outline,
        )
    }
}

/**
 * The circle behind the back button, which is more prominent when the header is at rest and less
 * so once it has scrolled under the bar. It is drawn, so a scroll fades it without recomposing.
 *
 * @param color the circle's fill, before the fade.
 * @param collapsedFraction how far the header has scrolled under the bar, from 0 at rest to 1 once
 * it is past.
 */
private fun Modifier.fadingBackground(
    color: Color,
    collapsedFraction: () -> Float,
): Modifier =
    drawBehind {
        drawOutline(
            outline =
                CircleShape.createOutline(
                    size = size,
                    layoutDirection = layoutDirection,
                    density = this,
                ),
            color = color,
            alpha = (1f - collapsedFraction()) * BACK_BUTTON_ALPHA_MULTIPLIER,
        )
    }

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
