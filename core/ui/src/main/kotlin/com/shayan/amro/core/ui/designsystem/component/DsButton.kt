package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.R
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.preview.DsButtonGallery
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.ComponentShapes
import com.shayan.amro.core.ui.designsystem.theme.Motion
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.CircularProgressIndicator as M3CircularProgressIndicator
import androidx.compose.material3.ElevatedButton as M3ElevatedButton
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.TextButton as M3TextButton

/** The glyph in either slot, which the spinner that replaces the trailing one matches. */
private val BUTTON_ICON_SIZE = 18.dp

enum class ButtonVariant { Filled, Tonal, Outlined, Text, Elevated }

/**
 * Design-system button wrapping the M3 variants, pressed-scale animation, and a [loading] state.
 *
 * @param text button label.
 * @param onClick invoked on click; a no-op while [loading].
 * @param variant one of the five M3 button styles.
 * @param enabled `false` dims and disables the button.
 * @param loading `true` shows a trailing spinner and blocks the click while keeping full colour.
 * @param leadingGlyph optional [com.shayan.amro.core.ui.designsystem.icon.Glyphs] constant before the label.
 * @param trailingGlyph optional [com.shayan.amro.core.ui.designsystem.icon.Glyphs] constant after the label; hidden while [loading].
 */
@Composable
fun DsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingGlyph: String? = null,
    trailingGlyph: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.pressScaleButton else 1f,
        animationSpec = tween(Motion.durationShortMillis, easing = Motion.easingStandard),
        label = "button-press-scale",
    )
    val loadingDescription = stringResource(R.string.core_ui_loading)
    val pressModifier =
        modifier
            .graphicsLayer {
                // Lambda graphicsLayer only re-draws, has better performance than Modifier.scale()
                scaleX = scale
                scaleY = scale
            }.semantics { if (loading) stateDescription = loadingDescription }

    val action = if (loading) ({}) else onClick
    val shape = ComponentShapes.button
    val content = buttonContent(text, loading, leadingGlyph, trailingGlyph)
    val contentPadding =
        when {
            variant == ButtonVariant.Text -> {
                ButtonDefaults.TextButtonContentPadding
            }

            leadingGlyph != null || trailingGlyph != null -> {
                PaddingValues(horizontal = Spacing.s5, vertical = Spacing.s2)
            }

            else -> {
                ButtonDefaults.ContentPadding
            }
        }
    when (variant) {
        ButtonVariant.Filled -> {
            M3Button(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                content = content,
            )
        }

        ButtonVariant.Tonal -> {
            M3FilledTonalButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                content = content,
            )
        }

        ButtonVariant.Outlined -> {
            M3OutlinedButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                content = content,
            )
        }

        ButtonVariant.Text -> {
            M3TextButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                content = content,
            )
        }

        ButtonVariant.Elevated -> {
            M3ElevatedButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                content = content,
            )
        }
    }
}

/**
 * Builds the row content shared by every [ButtonVariant].
 */
private fun buttonContent(
    text: String,
    loading: Boolean,
    leadingGlyph: String?,
    trailingGlyph: String?,
): @Composable RowScope.() -> Unit =
    {
        if (leadingGlyph != null) {
            DsIcon(leadingGlyph, contentDescription = null, size = BUTTON_ICON_SIZE)
            Spacer(Modifier.width(Spacing.s2))
        }
        Text(text)
        // The spinner occupies the trailing slot, replacing any trailing glyph while it spins.
        if (loading) {
            Spacer(Modifier.width(Spacing.s2))
            M3CircularProgressIndicator(
                modifier = Modifier.size(BUTTON_ICON_SIZE),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                trackColor = LocalContentColor.current.copy(alpha = 0.25f),
            )
        } else if (trailingGlyph != null) {
            Spacer(Modifier.width(Spacing.s2))
            DsIcon(trailingGlyph, contentDescription = null, size = BUTTON_ICON_SIZE)
        }
    }

@Preview
@Composable
private fun ButtonPreview() {
    AmroTheme(darkTheme = true) {
        Surface { DsButtonGallery() }
    }
}
