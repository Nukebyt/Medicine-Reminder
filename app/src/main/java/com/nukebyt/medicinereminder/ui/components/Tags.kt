package com.nukebyt.medicinereminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.util.labelRes

/** Round swatch for a medicine colour tag. */
@Composable
fun ColorTag(colorIndex: Int, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Box(modifier.size(size).background(AppTheme.colors.medicineColor(colorIndex), AppShapes.cell))
}

/** "Before meal" is amber; other relations use a quiet neutral chip. The label always carries the meaning. */
@Composable
fun MealTag(relation: MealRelation, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val before = relation == MealRelation.BEFORE_MEAL
    Box(
        modifier
            .background(if (before) colors.amber else colors.paper, AppShapes.tag)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(relation.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            color = if (before) colors.onAmber else colors.ink,
        )
    }
}

@ThemePreviews
@Composable
private fun TagsPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MealRelation.entries.take(3).forEach { MealTag(it) }
        (0..7).forEach { ColorTag(it) }
    }
}
