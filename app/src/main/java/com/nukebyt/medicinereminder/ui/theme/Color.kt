package com.nukebyt.medicinereminder.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colour tokens from ROADMAP section 7. Screens read colours from the theme only. */
private object Light {
    val ink = Color(0xFF1B1D3A)
    val paper = Color(0xFFF7F8FC)
    val foil = Color(0xFFE4E7F1)
    val capsule = Color(0xFF4C6FFF)
    val amber = Color(0xFFE8A317)
    val mint = Color(0xFF1FA57A)
    val alert = Color(0xFFD93B47)
    val muted = Color(0xFF6B7090)
}

private object Dark {
    val ink = Color(0xFFECEEF8)
    val paper = Color(0xFF14162B)
    val foil = Color(0xFF22264A)
    val capsule = Color(0xFF8FA4FF)
    val amber = Color(0xFFFFC94D)
    val mint = Color(0xFF4FD8A8)
    val alert = Color(0xFFFF6B74)
    val muted = Color(0xFF8A8FB3)
}

/**
 * Colours that Material 3 has no slot for. In the light theme [mint] and [amber] are fills only
 * (contrast on the paper is under 3:1), so text on them uses [onMint] / [onAmber] and they are never used as text.
 */
@Immutable
data class AppColors(
    val ink: Color,
    val paper: Color,
    val foil: Color,
    val capsule: Color,
    val onCapsule: Color,
    val amber: Color,
    val onAmber: Color,
    val mint: Color,
    val onMint: Color,
    val alert: Color,
    val onAlert: Color,
    val muted: Color,
    /** The 8 medicine colour tags, each at least 3:1 against [foil]. */
    val medicine: List<Color>,
    val isDark: Boolean,
) {
    fun medicineColor(index: Int): Color = medicine[index.mod(medicine.size)]
}

val MedicineColorNames = listOf("Blue", "Mint", "Amber", "Coral", "Violet", "Teal", "Rose", "Slate")

private val medicineLight = listOf(
    Color(0xFF3F5CE0), Color(0xFF14876A), Color(0xFFA96F00), Color(0xFFC94A34),
    Color(0xFF7448CC), Color(0xFF0B7F8E), Color(0xFFC0356F), Color(0xFF586080),
)
private val medicineDark = listOf(
    Color(0xFF8FA4FF), Color(0xFF4FD8A8), Color(0xFFFFC94D), Color(0xFFFF8A73),
    Color(0xFFB697FF), Color(0xFF4FD0E0), Color(0xFFFF86B4), Color(0xFFA0A6CC),
)

val LightAppColors = AppColors(
    ink = Light.ink, paper = Light.paper, foil = Light.foil,
    capsule = Light.capsule, onCapsule = Color.White,
    amber = Light.amber, onAmber = Light.ink,
    mint = Light.mint, onMint = Color.White,
    alert = Light.alert, onAlert = Color.White,
    muted = Light.muted, medicine = medicineLight, isDark = false,
)

val DarkAppColors = AppColors(
    ink = Dark.ink, paper = Dark.paper, foil = Dark.foil,
    capsule = Dark.capsule, onCapsule = Dark.paper,
    amber = Dark.amber, onAmber = Dark.paper,
    mint = Dark.mint, onMint = Dark.paper,
    alert = Dark.alert, onAlert = Dark.paper,
    muted = Dark.muted, medicine = medicineDark, isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

fun AppColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = capsule,
        onPrimary = onCapsule,
        primaryContainer = foil,
        onPrimaryContainer = ink,
        secondary = muted,
        onSecondary = paper,
        secondaryContainer = foil,
        onSecondaryContainer = ink,
        tertiary = mint,
        onTertiary = onMint,
        background = paper,
        onBackground = ink,
        surface = paper,
        onSurface = ink,
        surfaceVariant = foil,
        onSurfaceVariant = muted,
        surfaceTint = capsule,
        surfaceContainerLowest = paper,
        surfaceContainerLow = paper,
        surfaceContainer = foil,
        surfaceContainerHigh = foil,
        surfaceContainerHighest = foil,
        error = alert,
        onError = onAlert,
        errorContainer = foil,
        onErrorContainer = alert,
        outline = muted,
        outlineVariant = foil,
        scrim = Color.Black,
    )
}
