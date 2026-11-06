package com.nukebyt.medicinereminder.ui.medicines

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Sanitizer
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.MedicineFormError
import com.nukebyt.medicinereminder.domain.model.DaysMask
import com.nukebyt.medicinereminder.domain.model.MealRelation
import com.nukebyt.medicinereminder.domain.model.MedForm
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.util.formatTime
import com.nukebyt.medicinereminder.ui.util.labelRes
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val ColorNameRes = listOf(
    R.string.color_blue, R.string.color_mint, R.string.color_amber, R.string.color_coral,
    R.string.color_violet, R.string.color_teal, R.string.color_rose, R.string.color_slate,
)

private fun MedForm.icon(): ImageVector = when (this) {
    MedForm.TABLET, MedForm.CAPSULE -> Icons.Rounded.Medication
    MedForm.SYRUP -> Icons.Rounded.LocalDrink
    MedForm.DROPS -> Icons.Rounded.WaterDrop
    MedForm.INJECTION -> Icons.Rounded.Vaccines
    MedForm.INHALER -> Icons.Rounded.Air
    MedForm.CREAM -> Icons.Rounded.Sanitizer
    MedForm.OTHER -> Icons.Rounded.Healing
}

@StringRes
private fun MedicineFormError.messageRes(): Int = when (this) {
    MedicineFormError.NAME_REQUIRED -> R.string.error_name_required
    MedicineFormError.TIME_REQUIRED -> R.string.error_time_required
    MedicineFormError.DUPLICATE_TIME -> R.string.error_duplicate_time
    MedicineFormError.END_BEFORE_START -> R.string.error_end_before_start
    MedicineFormError.NO_DAYS_SELECTED -> R.string.error_no_days
    MedicineFormError.DURATION_INVALID -> R.string.error_duration_invalid
}

private fun LocalDate.pretty(): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicineScreen(
    onBack: () -> Unit,
    viewModel: EditMedicineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (state.isEditing) R.string.edit_title_edit else R.string.edit_title_add),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding))
        } else {
            EditMedicineForm(state, viewModel, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditMedicineForm(state: EditMedicineUiState, vm: EditMedicineViewModel, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    var showTimePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var mealMore by remember(state.isEditing) { mutableStateOf(state.mealRelation == MealRelation.EMPTY_STOMACH) }

    fun has(e: MedicineFormError) = state.showErrors && e in state.errors

    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ---- name ----
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onNameChange,
                label = { Text(stringResource(R.string.edit_name)) },
                singleLine = true,
                isError = has(MedicineFormError.NAME_REQUIRED),
                supportingText = if (has(MedicineFormError.NAME_REQUIRED)) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.nameSuggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.nameSuggestions.forEach { name ->
                        SuggestionChip(onClick = { vm.onSuggestionPicked(name) }, label = { Text(name) })
                    }
                }
            }
        }

        // ---- dosage ----
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = state.dosageAmount,
                onValueChange = vm::onDosageAmountChange,
                label = { Text(stringResource(R.string.edit_dosage_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            UnitDropdown(state.dosageUnit, vm::onUnitChange, Modifier.weight(1f))
        }

        // ---- form ----
        FormSection(R.string.edit_form) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MedForm.entries.forEach { form ->
                    FilterChip(
                        selected = state.form == form,
                        onClick = { vm.onFormChange(form) },
                        label = { Text(stringResource(form.labelRes())) },
                        leadingIcon = { Icon(form.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }

        // ---- meal relation ----
        FormSection(R.string.edit_meal) {
            val main = listOf(MealRelation.BEFORE_MEAL, MealRelation.AFTER_MEAL, MealRelation.WITH_MEAL, MealRelation.ANY_TIME)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                main.forEachIndexed { index, relation ->
                    SegmentedButton(
                        selected = state.mealRelation == relation,
                        onClick = { vm.onMealChange(relation) },
                        shape = SegmentedButtonDefaults.itemShape(index, main.size),
                        label = { Text(stringResource(relation.labelRes()), maxLines = 2) },
                    )
                }
            }
            if (mealMore || state.mealRelation == MealRelation.EMPTY_STOMACH) {
                FilterChip(
                    selected = state.mealRelation == MealRelation.EMPTY_STOMACH,
                    onClick = { vm.onMealChange(MealRelation.EMPTY_STOMACH) },
                    label = { Text(stringResource(MealRelation.EMPTY_STOMACH.labelRes())) },
                )
            } else {
                AssistChip(
                    onClick = { mealMore = true },
                    label = { Text(stringResource(R.string.edit_meal_more)) },
                    leadingIcon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }

        // ---- times ----
        FormSection(R.string.edit_times) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.times.forEach { t ->
                    val label = formatTime(t)
                    InputChip(
                        selected = true,
                        onClick = { vm.removeTime(t) },
                        label = { Text(label) },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.edit_remove_time, label),
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
                AssistChip(
                    onClick = { showTimePicker = true },
                    label = { Text(stringResource(R.string.edit_add_time)) },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTimes.forEach { (kind, time) ->
                    val res = when (kind) {
                        QuickTime.MORNING -> R.string.edit_quick_morning
                        QuickTime.AFTERNOON -> R.string.edit_quick_afternoon
                        QuickTime.NIGHT -> R.string.edit_quick_night
                    }
                    SuggestionChip(onClick = { vm.addTime(time) }, label = { Text(stringResource(res, formatTime(time))) })
                }
            }
            val timeError = when {
                has(MedicineFormError.DUPLICATE_TIME) -> MedicineFormError.DUPLICATE_TIME
                has(MedicineFormError.TIME_REQUIRED) -> MedicineFormError.TIME_REQUIRED
                else -> null
            }
            if (timeError != null) ErrorText(timeError)
        }

        // ---- repeat ----
        FormSection(R.string.edit_repeat) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.everyDay,
                    onClick = vm::onEveryDay,
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = { Text(stringResource(R.string.edit_every_day)) },
                )
                SegmentedButton(
                    selected = !state.everyDay,
                    onClick = vm::onSpecificDays,
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = { Text(stringResource(R.string.edit_specific_days)) },
                )
            }
            if (!state.everyDay) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = DaysMask.contains(state.daysMask, day),
                            onClick = { vm.toggleDay(day) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                        )
                    }
                }
            }
            if (has(MedicineFormError.NO_DAYS_SELECTED)) ErrorText(MedicineFormError.NO_DAYS_SELECTED)
        }

        // ---- dates ----
        FormSection(R.string.edit_start_date) {
            OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.startDate.pretty())
            }
        }
        FormSection(R.string.edit_end) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.endMode == EndMode.ONGOING,
                    onClick = { vm.onEndModeChange(EndMode.ONGOING) },
                    label = { Text(stringResource(R.string.edit_end_ongoing)) },
                )
                FilterChip(
                    selected = state.endMode == EndMode.DATE,
                    onClick = { vm.onEndModeChange(EndMode.DATE) },
                    label = { Text(stringResource(R.string.edit_end_date)) },
                )
                FilterChip(
                    selected = state.endMode == EndMode.DAYS,
                    onClick = { vm.onEndModeChange(EndMode.DAYS) },
                    label = { Text(stringResource(R.string.edit_end_days)) },
                )
            }
            when (state.endMode) {
                EndMode.DATE -> OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.endDate?.pretty() ?: stringResource(R.string.edit_end_date_label))
                }
                EndMode.DAYS -> OutlinedTextField(
                    value = state.durationDays,
                    onValueChange = vm::onDurationChange,
                    label = { Text(stringResource(R.string.edit_duration_days)) },
                    singleLine = true,
                    isError = has(MedicineFormError.DURATION_INVALID),
                    supportingText = state.effectiveEndDate?.let { end -> { Text(end.pretty()) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                EndMode.ONGOING -> Unit
            }
            if (has(MedicineFormError.END_BEFORE_START)) ErrorText(MedicineFormError.END_BEFORE_START)
            if (has(MedicineFormError.DURATION_INVALID)) ErrorText(MedicineFormError.DURATION_INVALID)
        }

        // ---- colour ----
        FormSection(R.string.edit_color) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.medicine.forEachIndexed { index, color ->
                    val name = stringResource(ColorNameRes[index])
                    val selected = state.colorIndex == index
                    Box(
                        Modifier
                            .size(48.dp)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.onColorChange(index) })
                            .semantics { contentDescription = name },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(color, AppShapes.cell)
                                .then(if (selected) Modifier.border(3.dp, colors.ink, AppShapes.cell) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.paper, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // ---- notes ----
        OutlinedTextField(
            value = state.notes,
            onValueChange = vm::onNotesChange,
            label = { Text(stringResource(R.string.edit_notes)) },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = vm::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Text(stringResource(R.string.edit_save), style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { vm.addTime(it); showTimePicker = false },
        )
    }
    if (showStartPicker) {
        DatePickerDialogFor(state.startDate, onDismiss = { showStartPicker = false }) { vm.onStartDateChange(it); showStartPicker = false }
    }
    if (showEndPicker) {
        DatePickerDialogFor(state.endDate ?: state.startDate, onDismiss = { showEndPicker = false }) { vm.onEndDateChange(it); showEndPicker = false }
    }
}

@Composable
private fun FormSection(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.ink)
        content()
    }
}

@Composable
private fun ErrorText(error: MedicineFormError) {
    Text(stringResource(error.messageRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(unit: String, onUnitChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(unit) { if (unit in DosageUnits) DosageUnits else DosageUnits + unit }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = unit,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.edit_dosage_unit)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onUnitChange(option); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val picker = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_time_picker_title)) },
        text = { TimePicker(state = picker) },
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(picker.hour, picker.minute)) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(state = state) }
}
