package com.nukebyt.medicinereminder.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.ui.components.PreviewSurface
import com.nukebyt.medicinereminder.ui.components.ThemePreviews
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme

/** Shown on Today while a critical permission (notifications, exact alarms) is missing. */
@Composable
fun PermissionBanner(onFix: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Surface(modifier.fillMaxWidth(), shape = AppShapes.card, color = colors.alert, contentColor = colors.onAlert) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.banner_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.banner_message), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onFix) {
                Text(stringResource(R.string.banner_action), color = colors.onAlert, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PermissionBannerPreview() = PreviewSurface { PermissionBanner(onFix = {}) }
