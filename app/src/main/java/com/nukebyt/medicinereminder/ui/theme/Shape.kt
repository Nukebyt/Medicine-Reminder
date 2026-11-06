package com.nukebyt.medicinereminder.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Different radii for different hierarchy levels, on purpose. */
object AppShapes {
    val card = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val capsule = RoundedCornerShape(percent = 50)
    val cell = CircleShape
    val tag = RoundedCornerShape(8.dp)
}

val AppMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = AppShapes.card,
    large = RoundedCornerShape(24.dp),
    extraLarge = AppShapes.sheet,
)
