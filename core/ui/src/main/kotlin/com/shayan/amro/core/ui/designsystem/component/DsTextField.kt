package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.preview.DsTextFieldGallery
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.ComponentShapes
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField
import androidx.compose.material3.TextField as M3TextField

/** The glyph in either field slot. */
private val FIELD_ICON_SIZE = 20.dp

enum class TextFieldVariant { Outlined, Filled }

/**
 * Design-system text field wrapping the M3 outlined / filled variants.
 *
 * The floating label and the 2dp accent focus border come from the M3 defaults, not re-implemented
 * here. All copy ([label], [placeholder], [supportingText]) is caller-supplied; the field holds no
 * literal.
 *
 * @param label floating label; drawn inside the field at rest, animating up on focus/content.
 * @param variant outlined (default) or filled.
 * @param leadingGlyph / trailingGlyph [com.shayan.amro.core.ui.designsystem.icon.Glyphs] ligatures for the leading/trailing slots.
 * @param onTrailingClick when non-null, wraps the trailing glyph in a clickable icon button.
 * @param trailingContentDescription label for the trailing slot, required whenever
 *   [onTrailingClick] makes it interactive.
 * @param supportingText helper/error text below the field.
 * @param visualTransformation e.g. password masking.
 * @param keyboardActions what the IME action does, e.g. submitting a key from the keyboard.
 */
@Composable
fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    variant: TextFieldVariant = TextFieldVariant.Outlined,
    leadingGlyph: String? = null,
    trailingGlyph: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    trailingContentDescription: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val labelComposable: (@Composable () -> Unit)? = label?.let { { Text(it) } }
    val placeholderComposable: (@Composable () -> Unit)? = placeholder?.let { { Text(it) } }
    val supportingComposable: (@Composable () -> Unit)? = supportingText?.let { { Text(it) } }
    val leadingComposable: (@Composable () -> Unit)? =
        leadingGlyph?.let {
            { DsIcon(it, contentDescription = null, size = FIELD_ICON_SIZE) }
        }
    val trailingComposable: (@Composable () -> Unit)? =
        trailingGlyph?.let { glyph ->
            {
                if (onTrailingClick != null) {
                    M3IconButton(
                        onClick = onTrailingClick,
                    ) {
                        DsIcon(
                            glyph,
                            contentDescription = trailingContentDescription,
                            size = FIELD_ICON_SIZE,
                        )
                    }
                } else {
                    DsIcon(
                        glyph,
                        contentDescription = trailingContentDescription,
                        size = FIELD_ICON_SIZE,
                    )
                }
            }
        }
    when (variant) {
        TextFieldVariant.Outlined -> {
            M3OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                label = labelComposable,
                placeholder = placeholderComposable,
                leadingIcon = leadingComposable,
                trailingIcon = trailingComposable,
                supportingText = supportingComposable,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                minLines = minLines,
                shape = ComponentShapes.input,
            )
        }

        TextFieldVariant.Filled -> {
            M3TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                label = labelComposable,
                placeholder = placeholderComposable,
                leadingIcon = leadingComposable,
                trailingIcon = trailingComposable,
                supportingText = supportingComposable,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                minLines = minLines,
            )
        }
    }
}

@Preview
@Composable
private fun TextFieldPreview() {
    AmroTheme(darkTheme = true) {
        Surface { DsTextFieldGallery() }
    }
}
