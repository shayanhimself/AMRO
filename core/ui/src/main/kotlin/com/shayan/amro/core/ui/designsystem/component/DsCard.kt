package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.preview.DsCardGallery
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.ComponentShapes
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.ElevatedCard as M3ElevatedCard
import androidx.compose.material3.OutlinedCard as M3OutlinedCard

enum class CardVariant { Filled, Outlined, Elevated }

/**
 * Design-system card wrapping the three M3 card variants.
 *
 * @param variant one of filled (default) / outlined / elevated.
 * @param onClick invoked on click; non-null turns the card into a clickable surface.
 * @param content card body, laid out in a [ColumnScope].
 */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Filled,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = ComponentShapes.card
    when (variant) {
        CardVariant.Filled -> {
            if (onClick != null) {
                M3Card(onClick = onClick, modifier = modifier, shape = shape, content = content)
            } else {
                M3Card(modifier = modifier, shape = shape, content = content)
            }
        }

        CardVariant.Outlined -> {
            if (onClick != null) {
                M3OutlinedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    content = content,
                )
            } else {
                M3OutlinedCard(modifier = modifier, shape = shape, content = content)
            }
        }

        CardVariant.Elevated -> {
            if (onClick != null) {
                M3ElevatedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    content = content,
                )
            } else {
                M3ElevatedCard(modifier = modifier, shape = shape, content = content)
            }
        }
    }
}

@Preview
@Composable
private fun CardPreview() {
    AmroTheme(darkTheme = true) {
        Surface { DsCardGallery() }
    }
}
