package com.shayan.amro.feature.trending.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.component.DsButton
import com.shayan.amro.core.ui.designsystem.icon.DsIcon
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.designsystem.theme.Spacing
import com.shayan.amro.feature.trending.ui.TrendingPreviewData
import com.shayan.amro.feature.trending.viewmodel.NoticeUiState
import com.shayan.amro.core.ui.R as CoreUiR

/**
 * Shows a bar that says what happened, and lets the user retry.
 *
 * @param notice what the bar draws and says.
 * @param onRetry retries.
 */
@Composable
internal fun TrendingNoticeBar(
    notice: NoticeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(
                modifier =
                    Modifier.padding(
                        start = Spacing.s4,
                        end = Spacing.s2,
                        top = Spacing.s3,
                        bottom = Spacing.s3,
                    ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DsIcon(
                    glyph = notice.glyph,
                    // The sentence beside it says what happened.
                    contentDescription = null,
                    size = 20.dp,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = notice.message.resolve(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                DsButton(
                    text = stringResource(CoreUiR.string.core_ui_retry),
                    onClick = onRetry,
                    variant = ButtonVariant.Text,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Preview
@Composable
private fun TrendingNoticeBarPreview() {
    AmroTheme {
        Surface {
            TrendingNoticeBar(notice = TrendingPreviewData.NOTICE, onRetry = {})
        }
    }
}
