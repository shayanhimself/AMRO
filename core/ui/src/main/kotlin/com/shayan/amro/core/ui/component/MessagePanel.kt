package com.shayan.amro.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.R
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.component.DsButton
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing

/** The disc the glyph sits in, and the glyph inside it. */
private val PANEL_GLYPH_DISC_SIZE = 72.dp
private val PANEL_GLYPH_SIZE = 36.dp

/**
 * Shows errors, and other situations that leave a screen without content.
 *
 * @param glyph the icon naming the situation.
 * @param title one line saying what happened.
 * @param actionLabel the one way out.
 * @param onAction takes it.
 * @param body a sentence under [title], or null where the title says everything.
 * @param glyphTint the glyph's colour, which the error causes raise.
 * @param actionVariant how the action is weighted against the message.
 * @param actionGlyph the icon on the action.
 */
@Composable
fun MessagePanel(
    glyph: String,
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    glyphTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actionVariant: ButtonVariant = ButtonVariant.Filled,
    actionGlyph: String = Glyphs.REFRESH,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.s6, vertical = Spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s4, Alignment.CenterVertically),
    ) {
        Box(
            modifier =
                Modifier
                    .size(PANEL_GLYPH_DISC_SIZE)
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            DsIcon(
                glyph = glyph,
                // The title states the icon already.
                contentDescription = null,
                size = PANEL_GLYPH_SIZE,
                tint = glyphTint,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        DsButton(
            text = actionLabel,
            onClick = onAction,
            variant = actionVariant,
            leadingGlyph = actionGlyph,
        )
    }
}

@Preview
@Composable
private fun MessagePanelPreview() {
    AmroTheme {
        Surface {
            MessagePanel(
                glyph = Glyphs.CLOUD_OFF,
                title = "No connection",
                actionLabel = stringResource(R.string.core_ui_retry),
                onAction = {},
                glyphTint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview
@Composable
private fun MessagePanelWithBodyPreview() {
    AmroTheme {
        Surface {
            MessagePanel(
                glyph = Glyphs.FILTER_ALT_OFF,
                title = "Nothing matches",
                actionLabel = "Clear filters",
                onAction = {},
                body = "Filters look inside what is already here. They don't search wider.",
                actionVariant = ButtonVariant.Tonal,
                actionGlyph = Glyphs.FILTER_ALT_OFF,
            )
        }
    }
}
