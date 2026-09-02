package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.designsystem.preview.DsBadgeGallery
import com.shayan.amro.core.ui.designsystem.theme.AmroExtendedTheme
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing

enum class BadgeTone { Primary, Error, Success, Neutral }

/**
 * Small attention marker: a count/label pill, or a bare dot when [text] is null.
 *
 * @param tone color role; defaults to `Error` because an unqualified badge is an attention marker,
 *   not a brand accent.
 * @param text badge label; `null` renders a dot instead.
 */
@Composable
fun DsBadge(
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Error,
    text: String? = null,
) {
    val extendedColors = AmroExtendedTheme.colors
    val colors = MaterialTheme.colorScheme
    val (container, onContainer) =
        when (tone) {
            BadgeTone.Primary -> colors.primary to colors.onPrimary
            BadgeTone.Error -> colors.error to colors.onError
            BadgeTone.Success -> extendedColors.success to extendedColors.onSuccess
            BadgeTone.Neutral -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        }
    if (text == null) {
        Box(modifier.size(8.dp).background(container, CircleShape))
    } else {
        Box(
            modifier
                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                .background(container, CircleShape)
                .padding(horizontal = Spacing.s1),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = onContainer, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview
@Composable
private fun BadgePreview() {
    AmroTheme(darkTheme = true) {
        Surface { DsBadgeGallery() }
    }
}
