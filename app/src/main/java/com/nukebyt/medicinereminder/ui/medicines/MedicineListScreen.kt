package com.nukebyt.medicinereminder.ui.medicines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.domain.model.Medicine
import com.nukebyt.medicinereminder.ui.components.ColorTag
import com.nukebyt.medicinereminder.ui.components.EmptyState
import com.nukebyt.medicinereminder.ui.components.SectionHeader
import com.nukebyt.medicinereminder.ui.theme.AppShapes
import com.nukebyt.medicinereminder.ui.theme.AppTheme
import com.nukebyt.medicinereminder.ui.util.formatTime
import com.nukebyt.medicinereminder.ui.util.labelRes

@Composable
fun MedicineListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: MedicineListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!state.isEmpty) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_medicine)) },
                    containerColor = AppTheme.colors.capsule,
                    contentColor = AppTheme.colors.onCapsule,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.isLoaded -> Unit
                state.isEmpty -> EmptyState(
                    title = stringResource(R.string.empty_medicines_title),
                    message = stringResource(R.string.empty_medicines_message),
                    actionLabel = stringResource(R.string.add_medicine),
                    onAction = onAdd,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> MedicineList(state, onEdit, viewModel::setPaused, viewModel::delete)
            }
        }
    }
}

@Composable
private fun MedicineList(
    state: MedicineListUiState,
    onEdit: (Long) -> Unit,
    onPause: (Medicine, Boolean) -> Unit,
    onDelete: (Medicine) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Text(stringResource(R.string.list_title), style = MaterialTheme.typography.headlineMedium, color = AppTheme.colors.ink)
        }
        medicineGroup(R.string.list_group_active, state.active, "active", onEdit, onPause, onDelete)
        medicineGroup(R.string.list_group_paused, state.paused, "paused", onEdit, onPause, onDelete)
        medicineGroup(R.string.list_group_finished, state.finished, "finished", onEdit, onPause, onDelete)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.medicineGroup(
    titleRes: Int,
    medicines: List<Medicine>,
    keyPrefix: String,
    onEdit: (Long) -> Unit,
    onPause: (Medicine, Boolean) -> Unit,
    onDelete: (Medicine) -> Unit,
) {
    if (medicines.isEmpty()) return
    item(key = "header-$keyPrefix") { SectionHeader(stringResource(titleRes)) }
    items(medicines, key = { it.id }) { medicine ->
        MedicineRow(medicine, onClick = { onEdit(medicine.id) }, onPause = onPause, onDelete = onDelete)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicineRow(
    medicine: Medicine,
    onClick: () -> Unit,
    onPause: (Medicine, Boolean) -> Unit,
    onDelete: (Medicine) -> Unit,
) {
    val colors = AppTheme.colors
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(medicine)
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(colors.alert, AppShapes.card).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete), tint = colors.onAlert)
            }
        },
    ) {
        Surface(shape = AppShapes.card, color = colors.foil, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .clickable(onClick = onClick)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
                    .alpha(if (medicine.isPaused) 0.6f else 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorTag(medicine.colorIndex, size = 20.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = listOf(medicine.name, medicine.dosageText).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.list_times_meal,
                            medicine.times.joinToString(", ") { formatTime(it) },
                            stringResource(medicine.mealRelation.labelRes()),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                RowMenu(medicine, onClick, onPause, onDelete)
            }
        }
    }
}

@Composable
private fun RowMenu(
    medicine: Medicine,
    onEdit: () -> Unit,
    onPause: (Medicine, Boolean) -> Unit,
    onDelete: (Medicine) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.list_more_options, medicine.name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { open = false; onEdit() })
            DropdownMenuItem(
                text = { Text(stringResource(if (medicine.isPaused) R.string.action_resume else R.string.action_pause)) },
                onClick = { open = false; onPause(medicine, !medicine.isPaused) },
            )
            DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { open = false; onDelete(medicine) })
        }
    }
}
