package de.ingomc.nozio.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.ingomc.nozio.data.repository.AppThemeMode
import de.ingomc.nozio.notifications.MealReminderReceiver
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showThemeMenu by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onMealReminderEnabledChange(granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    }
    val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val appBarState = rememberTopAppBarState()
    val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.mealReminderHour,
            initialMinute = state.mealReminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Erinnerungszeit") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        viewModel.onMealReminderTimeChange(timePickerState.hour, timePickerState.minute)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(appBarScrollBehavior.nestedScrollConnection)
    ) {
        TopAppBar(
            scrollBehavior = appBarScrollBehavior,
            title = {
                Text(
                    text = "Einstellungen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aenderungen werden sofort angewendet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingRow(
                title = "Aussehen",
                subtitle = "System, Hell oder Dunkel"
            ) {
                Box {
                    OutlinedButton(onClick = { showThemeMenu = true }) {
                        Text(state.themeMode.displayLabel())
                    }
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.displayLabel()) },
                                onClick = {
                                    showThemeMenu = false
                                    viewModel.onThemeModeChange(mode)
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            SettingRow(
                title = "Erinnerung",
                subtitle = "Taegliche Tracking-Benachrichtigung"
            ) {
                Switch(
                    checked = state.mealReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onMealReminderEnabledChange(enabled)
                        }
                    }
                )
            }

            if (state.mealReminderEnabled) {
                SettingRow(
                    title = "Uhrzeit",
                    subtitle = "Wann soll erinnert werden?"
                ) {
                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Text(formatTimeLabel(state.mealReminderHour, state.mealReminderMinute))
                    }
                }

                Button(
                    onClick = {
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            MealReminderReceiver.showNotification(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test-Reminder jetzt senden")
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        trailing()
    }
}

private fun AppThemeMode.displayLabel(): String = when (this) {
    AppThemeMode.SYSTEM -> "System"
    AppThemeMode.LIGHT -> "Hell"
    AppThemeMode.DARK -> "Dunkel"
}

private fun formatTimeLabel(hour: Int, minute: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}
