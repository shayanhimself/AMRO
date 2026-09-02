package com.shayan.amro.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.shayan.amro.core.testing.preview.ThemePreviews
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme

@PreviewTest
@ThemePreviews
@Composable
private fun ButtonGalleryPreview() {
    AmroTheme {
        Surface {
            Column {
                DsButtonGallery()
                DsIconButtonGallery()
            }
        }
    }
}
