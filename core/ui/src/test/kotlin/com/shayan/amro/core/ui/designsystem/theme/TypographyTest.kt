package com.shayan.amro.core.ui.designsystem.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test
    fun `display and headline are regular weight`() {
        assertEquals(57.sp, AmroTypography.displayLarge.fontSize)
        assertEquals(64.sp, AmroTypography.displayLarge.lineHeight)
        assertEquals(FontWeight.Normal, AmroTypography.displayLarge.fontWeight)
        assertEquals(32.sp, AmroTypography.headlineLarge.fontSize)
        assertEquals(40.sp, AmroTypography.headlineLarge.lineHeight)
        assertEquals(FontWeight.Normal, AmroTypography.headlineLarge.fontWeight)
    }

    @Test
    fun `title large is regular, title medium and labels are medium weight`() {
        assertEquals(22.sp, AmroTypography.titleLarge.fontSize)
        assertEquals(FontWeight.Normal, AmroTypography.titleLarge.fontWeight)
        assertEquals(16.sp, AmroTypography.titleMedium.fontSize)
        assertEquals(FontWeight.Medium, AmroTypography.titleMedium.fontWeight)
        assertEquals(0.2.sp, AmroTypography.titleMedium.letterSpacing)
        assertEquals(14.sp, AmroTypography.labelLarge.fontSize)
        assertEquals(0.1.sp, AmroTypography.labelLarge.letterSpacing)
        assertEquals(FontWeight.Medium, AmroTypography.labelSmall.fontWeight)
        assertEquals(11.sp, AmroTypography.labelSmall.fontSize)
    }

    @Test
    fun `body metrics match spec`() {
        assertEquals(16.sp, AmroTypography.bodyLarge.fontSize)
        assertEquals(24.sp, AmroTypography.bodyLarge.lineHeight)
        assertEquals(0.5.sp, AmroTypography.bodyLarge.letterSpacing)
        assertEquals(14.sp, AmroTypography.bodyMedium.fontSize)
        assertEquals(0.2.sp, AmroTypography.bodyMedium.letterSpacing)
        assertEquals(12.sp, AmroTypography.bodySmall.fontSize)
        assertEquals(0.4.sp, AmroTypography.bodySmall.letterSpacing)
    }
}
