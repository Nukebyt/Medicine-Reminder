package com.nukebyt.medicinereminder.ui.util

import android.content.Context
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@StringRes
fun MealRelation.labelRes(): Int = when (this) {
    MealRelation.BEFORE_MEAL -> R.string.meal_before_meal
    MealRelation.AFTER_MEAL -> R.string.meal_after_meal
    MealRelation.WITH_MEAL -> R.string.meal_with_meal
    MealRelation.EMPTY_STOMACH -> R.string.meal_empty_stomach
    MealRelation.ANY_TIME -> R.string.meal_any_time
}

@StringRes
fun MedForm.labelRes(): Int = when (this) {
    MedForm.TABLET -> R.string.form_tablet
    MedForm.CAPSULE -> R.string.form_capsule
    MedForm.SYRUP -> R.string.form_syrup
    MedForm.DROPS -> R.string.form_drops
    MedForm.INJECTION -> R.string.form_injection
    MedForm.INHALER -> R.string.form_inhaler
    MedForm.CREAM -> R.string.form_cream
    MedForm.OTHER -> R.string.form_other
}

private val timeFormatter: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** "8:00 am" in the user's locale. */
fun formatTime(time: LocalTime, locale: Locale = Locale.getDefault()): String =
    timeFormatter.withLocale(locale).format(time)
        .replace(' ', ' ')
        .replace("AM", "am").replace("PM", "pm")

/** True unless the user turned off animations (Settings > Remove animations). */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember { animationsEnabled(context) }
}

fun animationsEnabled(context: Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
} catch (_: Exception) {
    true
}
