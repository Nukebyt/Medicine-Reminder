package com.nukebyt.medicinereminder.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nukebyt.medicinereminder.domain.model.DoseState
import com.nukebyt.medicinereminder.domain.model.DoseUi
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.ui.theme.MedicineReminderTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Every composable gets these three previews (ROADMAP section 7, CLAUDE.md). */
@Preview(name = "Light", showBackground = true, widthDp = 380)
@Preview(name = "Dark", showBackground = true, widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Font 1.5x", showBackground = true, widthDp = 380, fontScale = 1.5f)
annotation class ThemePreviews

@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    MedicineReminderTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.padding(16.dp)) { content() }
        }
    }
}

internal object PreviewData {
    val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    val date: LocalDate = LocalDate.of(2026, 11, 6)
    val now: ZonedDateTime = date.atTime(8, 12).atZone(zone)

    fun dose(
        name: String = "Metformin",
        dosage: String = "500 mg",
        time: LocalTime = LocalTime.of(8, 0),
        state: DoseState = DoseState.UPCOMING,
        meal: MealRelation = MealRelation.AFTER_MEAL,
        form: MedForm = MedForm.TABLET,
        color: Int = 0,
    ) = DoseUi(
        medicineId = 1, medicineName = name, dosageText = dosage, form = form, mealRelation = meal,
        colorIndex = color, date = date, time = time, state = state,
        actionAt = if (state.isTaken) date.atTime(time).plusMinutes(4).atZone(zone).toInstant() else null,
    )
}
