package com.shayan.amro.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.designsystem.theme.AmroExtendedTheme
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.RadiusPrimitives
import com.shayan.amro.core.ui.designsystem.theme.Spacing

/**
 * A block of color that holds the place of content that is loading.
 *
 * @param color how strongly it reads against the surface behind it.
 * @param shape the corner it is cut to.
 */
@Composable
fun PlaceholderBlock(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RadiusPrimitives.radius1),
) {
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(color),
    )
}

@Preview
@Composable
private fun PlaceholderBlockPreview() {
    AmroTheme {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.s4),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                PlaceholderBlock(
                    color = AmroExtendedTheme.colors.placeholderMuted,
                    modifier = Modifier.fillMaxWidth().height(Spacing.s4),
                )
            }
        }
    }
}
