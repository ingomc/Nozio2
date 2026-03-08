package de.ingomc.nozio.ui.supplements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.repository.SupplementPlanItem
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementsEditScreen(
    viewModel: SupplementsEditViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var editingItem by remember { mutableStateOf<SupplementPlanItem?>(null) }
    var showEditorSheet by rememberSaveable { mutableStateOf(false) }

    if (showEditorSheet) {
        SupplementEditorSheet(
            item = editingItem,
            onDismiss = {
                showEditorSheet = false
                editingItem = null
            },
            onSave = { id, name, dayPart, scheduledMinutesOfDay, amountValue, amountUnit ->
                viewModel.savePlanItem(
                    id = id,
                    name = name,
                    dayPart = dayPart,
                    scheduledMinutesOfDay = scheduledMinutesOfDay,
                    amountValue = amountValue,
                    amountUnit = amountUnit
                )
                showEditorSheet = false
                editingItem = null
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Supplements bearbeiten",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        editingItem = null
                        showEditorSheet = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Supplement hinzufügen"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        state.errorMessage?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = viewModel::clearError) {
                        Text("OK")
                    }
                }
            }
        }

        val groupedByDayPart = remember(state.items) {
            SupplementDayPart.entries.associateWith { dayPart ->
                state.items
                    .filter { it.dayPart == dayPart }
                    .sortedBy { it.scheduledMinutesOfDay }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            SupplementDayPart.entries.forEach { dayPart ->
                item {
                    Text(
                        text = dayPart.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                }

                val sectionItems = groupedByDayPart[dayPart].orEmpty()
                if (sectionItems.isEmpty()) {
                    item {
                        Text(
                            text = "Keine Supplements in dieser Kategorie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    items(sectionItems, key = { it.id }) { item ->
                        SupplementPlanRow(
                            item = item,
                            onEdit = {
                                editingItem = item
                                showEditorSheet = true
                            },
                            onDelete = { viewModel.deletePlanItem(item.id) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SupplementPlanRow(
    item: SupplementPlanItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = LocalTime.of(
                        item.scheduledMinutesOfDay / 60,
                        item.scheduledMinutesOfDay % 60
                    ).format(timeFormatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${formatAmount(item.amountValue)} ${item.amountUnit.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Supplement bearbeiten"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supplement löschen"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplementEditorSheet(
    item: SupplementPlanItem?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        dayPart: SupplementDayPart,
        scheduledMinutesOfDay: Int,
        amountValue: Double,
        amountUnit: SupplementAmountUnit
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(item) { mutableStateOf(item?.name.orEmpty()) }
    var selectedDayPart by remember(item) { mutableStateOf(item?.dayPart ?: SupplementDayPart.PRE_BREAKFAST) }
    var selectedMinutes by remember(item) { mutableStateOf(item?.scheduledMinutesOfDay ?: 7 * 60) }
    var amountText by remember(item) { mutableStateOf(item?.amountValue?.toString().orEmpty()) }
    var selectedUnit by remember(item) { mutableStateOf(item?.amountUnit ?: SupplementAmountUnit.CAPSULE) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN) }
    val parsedAmount = amountText.replace(',', '.').toDoubleOrNull()
    val canSave = name.trim().isNotBlank() && (parsedAmount ?: 0.0) > 0.0

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedMinutes / 60,
            initialMinute = selectedMinutes % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Uhrzeit wählen") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedMinutes = (timePickerState.hour * 60) + timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (item == null) "Supplement hinzufügen" else "Supplement bearbeiten",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Kategorie",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SupplementDayPart.entries) { dayPart ->
                        FilterChip(
                            selected = selectedDayPart == dayPart,
                            onClick = { selectedDayPart = dayPart },
                            label = { Text(dayPart.displayName) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Uhrzeit: ${
                        LocalTime.of(selectedMinutes / 60, selectedMinutes % 60).format(timeFormatter)
                    }"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Menge") },
                    singleLine = true
                )

                Box {
                    OutlinedButton(onClick = { showUnitMenu = true }) {
                        Text(selectedUnit.displayName)
                    }
                    DropdownMenu(
                        expanded = showUnitMenu,
                        onDismissRequest = { showUnitMenu = false }
                    ) {
                        SupplementAmountUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    selectedUnit = unit
                                    showUnitMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        item?.id ?: 0,
                        name,
                        selectedDayPart,
                        selectedMinutes,
                        parsedAmount ?: 0.0,
                        selectedUnit
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }
        }
    }
}

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.GERMAN, "%.1f", value)
    }
}
