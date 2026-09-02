package com.shayan.amro.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.designsystem.component.BadgeTone
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.component.CardVariant
import com.shayan.amro.core.ui.designsystem.component.ChipVariant
import com.shayan.amro.core.ui.designsystem.component.DsBadge
import com.shayan.amro.core.ui.designsystem.component.DsButton
import com.shayan.amro.core.ui.designsystem.component.DsCard
import com.shayan.amro.core.ui.designsystem.component.DsChip
import com.shayan.amro.core.ui.designsystem.component.DsIconButton
import com.shayan.amro.core.ui.designsystem.component.DsSwitch
import com.shayan.amro.core.ui.designsystem.component.DsTextField
import com.shayan.amro.core.ui.designsystem.component.IconButtonVariant
import com.shayan.amro.core.ui.designsystem.component.TextFieldVariant
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.Spacing

// Galleries reused by components' colocated Previews and their screenshot tests:

/** Every button variant, plus the glyph slots and the disabled and loading states. */
@Composable
internal fun DsButtonGallery() {
    Column(
        modifier = Modifier.padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        ButtonVariant.entries.forEach { variant ->
            DsButton(
                text = variant.name,
                onClick = {},
                variant = variant,
                leadingGlyph = Glyphs.CLOSE,
            )
        }
        DsButton(text = "Continue", onClick = {}, trailingGlyph = Glyphs.ARROW_FORWARD)
        DsButton(text = "Disabled", onClick = {}, enabled = false)
        // Loading sits beside disabled on purpose: the golden is what proves they look different.
        DsButton(text = "Loading", onClick = {}, loading = true)
    }
}

/** Every icon button variant, plus the selected state the standard variant tints. */
@Composable
internal fun DsIconButtonGallery() {
    Row(modifier = Modifier.padding(Spacing.s4)) {
        DsIconButton(
            glyph = Glyphs.CLOSE,
            contentDescription = "Selected",
            onClick = {},
            selected = true,
        )
        IconButtonVariant.entries.forEach { variant ->
            DsIconButton(
                glyph = Glyphs.CLOSE,
                contentDescription = variant.name,
                onClick = {},
                variant = variant,
            )
        }
    }
}

/** Every card variant. */
@Composable
internal fun DsCardGallery() {
    Column(
        modifier = Modifier.padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        CardVariant.entries.forEach { variant ->
            DsCard(variant = variant) {
                Text(variant.name, Modifier.padding(Spacing.s4))
            }
        }
    }
}

/** Every badge tone as a labelled pill, and the bare dot a null label renders. */
@Composable
internal fun DsBadgeGallery() {
    Row(
        modifier = Modifier.padding(Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BadgeTone.entries.forEach { tone ->
            DsBadge(tone = tone, text = "3")
        }
        DsBadge()
    }
}

/** Both text field variants across the empty, error, clearable and read-only states. */
@Composable
internal fun DsTextFieldGallery() {
    Column(
        modifier = Modifier.padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        DsTextField(value = "", onValueChange = {}, label = "Label")
        DsTextField(
            value = "Blade Runner 2049",
            onValueChange = {},
            label = "Title",
            variant = TextFieldVariant.Filled,
        )
        DsTextField(
            value = "nope",
            onValueChange = {},
            label = "Year",
            isError = true,
            supportingText = "Not a year",
        )
        DsTextField(
            value = "clearable",
            onValueChange = {},
            label = "Search",
            // No preview shows the placeholder, it's painted just when focused+empty.
            placeholder = "Type to search",
            leadingGlyph = Glyphs.ARROW_FORWARD,
            trailingGlyph = Glyphs.CLOSE,
            onTrailingClick = {},
        )
        DsTextField(
            value = "read only",
            onValueChange = {},
            label = "Status",
            trailingGlyph = Glyphs.ERROR,
        )
    }
}

/** Both switch states. */
@Composable
internal fun DsSwitchGallery() {
    Row(
        modifier = Modifier.padding(Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        DsSwitch(checked = true, onCheckedChange = {})
        DsSwitch(checked = false, onCheckedChange = {})
    }
}

/** Every chip variant, with the filter chip selected and the input chip dismissible. */
@Composable
internal fun DsChipGallery() {
    Row(
        modifier = Modifier.padding(Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        ChipVariant.entries.forEach { variant ->
            DsChip(
                label = variant.name,
                onClick = {},
                variant = variant,
                selected = variant == ChipVariant.Filter,
                onDismiss =
                    if (variant == ChipVariant.Input) {
                        fun() {}
                    } else {
                        null
                    },
            )
        }
    }
}

/** One glyph per variable-font axis the icon exposes: fill, weight and optical size. */
@Composable
internal fun DsIconGallery() {
    Row(
        modifier = Modifier.padding(Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DsIcon(Glyphs.CLOSE, contentDescription = null)
        DsIcon(Glyphs.ERROR, contentDescription = null, filled = true)
        DsIcon(Glyphs.ARROW_BACK, contentDescription = null, weight = 600)
        DsIcon(Glyphs.CHECK, contentDescription = null, filled = true, size = 36.dp)
    }
}
