package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayan.amro.core.ui.designsystem.preview.DsSwitchGallery
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import androidx.compose.material3.Switch as M3Switch

/**
 * Design-system toggle switch: a thin wrapper over the M3 [M3Switch] with our theme colors.
 *
 * @param checked whether the switch is on.
 * @param onCheckedChange invoked with the new state on toggle.
 */
@Composable
fun DsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    M3Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Preview
@Composable
private fun SwitchPreview() {
    AmroTheme(darkTheme = true) {
        Surface { DsSwitchGallery() }
    }
}
