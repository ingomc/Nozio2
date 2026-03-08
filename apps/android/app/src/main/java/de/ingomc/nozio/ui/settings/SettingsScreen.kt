package de.ingomc.nozio.ui.settings

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import de.ingomc.nozio.BuildConfig
import de.ingomc.nozio.data.repository.AppThemeMode
import de.ingomc.nozio.notifications.MealReminderReceiver
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    section: SettingsSection = SettingsSection.MAIN,
    onNavigateToReminder: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToLegalInfo: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showThemeMenu by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onMealReminderEnabledChange(granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    }
    val unknownSourcesSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onInstallDownloadedUpdateClicked()
    }
    val driveAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDriveAuthorizationResult(result.resultCode, result.data)
    }
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri ->
        viewModel.onBackupExportDestinationSelected(uri)
    }
    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onBackupImportSourceSelected(uri)
    }
    val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val appBarState = rememberTopAppBarState()
    val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)

    LaunchedEffect(Unit) {
        viewModel.onSettingsOpened()
    }

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.OpenUrl -> openUrl(context, effect.url)
                SettingsEffect.OpenUnknownSourcesSettings -> {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                    runCatching { unknownSourcesSettingsLauncher.launch(intent) }
                }

                is SettingsEffect.StartInstall -> {
                    runCatching { context.startActivity(effect.intent) }
                }

                SettingsEffect.LaunchCredentialManagerSignIn -> {
                    scope.launch {
                        runCredentialManagerSignIn(context, viewModel)
                    }
                }

                is SettingsEffect.LaunchDriveAuthorization -> {
                    runCatching {
                        val request = IntentSenderRequest.Builder(effect.intentSender).build()
                        driveAuthorizationLauncher.launch(request)
                    }.onFailure {
                        viewModel.onCredentialManagerSignInFailed("Google Drive Autorisierung konnte nicht gestartet werden.")
                    }
                }

                is SettingsEffect.LaunchBackupExport -> {
                    backupExportLauncher.launch(effect.suggestedFileName)
                }

                SettingsEffect.LaunchBackupImport -> {
                    backupImportLauncher.launch(arrayOf("application/gzip", "application/json", "application/octet-stream"))
                }
            }
        }
    }

    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId > 0L) {
                    viewModel.onDownloadCompleted(downloadId)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

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

    if (state.showUpdateDialog && state.availableReleaseTitle != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onUpdateDialogDismissed() },
            title = { Text("Update verfuegbar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.availableReleaseTitle.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.availableReleaseNotesPreview.orEmpty(),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!state.hasDownloadableUpdate) {
                        Text(
                            text = "Dieses Release enthaelt keine passende APK fuer den Direktdownload.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                val label = if (state.hasDownloadableUpdate) "Herunterladen" else "Release-Seite"
                TextButton(
                    onClick = {
                        if (state.hasDownloadableUpdate) {
                            viewModel.onDownloadUpdateClicked()
                        } else {
                            viewModel.onOpenReleasePageClicked()
                        }
                        viewModel.onUpdateDialogDismissed()
                    }
                ) {
                    Text(label)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onOpenReleasePageClicked()
                        viewModel.onUpdateDialogDismissed()
                    }
                ) {
                    Text("Release-Seite")
                }
            }
        )
    }

    if (state.showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onRestoreDialogDismissed() },
            title = { Text("Backup wiederherstellen") },
            text = {
                Text("Dadurch werden alle lokalen Tracking-Daten ersetzt. Dieser Schritt kann nicht rueckgaengig gemacht werden.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onRestoreConfirmed() }) {
                    Text("Wiederherstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onRestoreDialogDismissed() }) {
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
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurueck"
                        )
                    }
                }
            },
            title = {
                Text(
                    text = section.title,
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
            when (section) {
                SettingsSection.MAIN -> {
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
                        subtitle = "Taegliche Tracking-Benachrichtigung",
                        onClick = onNavigateToReminder
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatReminderSummary(
                                    enabled = state.mealReminderEnabled,
                                    hour = state.mealReminderHour,
                                    minute = state.mealReminderMinute
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    SettingRow(
                        title = "Backup & Wiederherstellung",
                        subtitle = "Sicherung, Import und Export",
                        onClick = onNavigateToBackup
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatBackupSummary(state),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    SettingRow(
                        title = "Rechtliche Hinweise",
                        subtitle = "Impressum, Datenschutz und Lizenzinfos",
                        onClick = onNavigateToLegalInfo
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "App",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    SettingRow(
                        title = "Version",
                        subtitle = state.appVersionLabel
                    )

                    SettingRow(
                        title = "Updates",
                        subtitle = state.updateStatus.label()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onCheckForUpdateClicked() },
                            enabled = state.updateStatus != UpdateStatus.CHECKING
                        ) {
                            Text(if (state.updateStatus == UpdateStatus.CHECKING) "Pruefung..." else "Jetzt pruefen")
                        }
                    }

                    if (state.updateStatus == UpdateStatus.UPDATE_AVAILABLE && state.availableReleaseTitle != null) {
                        Text(
                            text = "Neuestes Release: ${state.availableReleaseTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (state.downloadInProgress) {
                            Text(
                                text = "Update wird heruntergeladen...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Button(
                                onClick = { viewModel.onDownloadUpdateClicked() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.hasDownloadableUpdate
                            ) {
                                Text(if (state.hasDownloadableUpdate) "Update herunterladen" else "Kein Direktdownload verfuegbar")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.onOpenReleasePageClicked() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Release-Seite oeffnen")
                        }
                    }

                    state.errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                SettingsSection.REMINDER -> {
                    SettingRow(
                        title = "Erinnerung aktivieren",
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

                SettingsSection.BACKUP -> {
                    SettingRow(
                        title = "Auto-Backup",
                        subtitle = "Woechentliche Sicherung als lokale Datei"
                    ) {
                        Switch(
                            checked = state.autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.onAutoBackupEnabledChange(enabled)
                            }
                        )
                    }

                    val accountLabel = state.backupConnectedAccount ?: "Nicht verbunden"
                    SettingRow(
                        title = "Backup-Speicher",
                        subtitle = accountLabel
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onDriveSignInClicked() },
                            enabled = !state.backupInProgress && !state.restoreInProgress
                        ) {
                            Text("Pruefen")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onExportBackupClicked() },
                            enabled = !state.backupInProgress && !state.restoreInProgress,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exportieren")
                        }
                        OutlinedButton(
                            onClick = { viewModel.onImportBackupClicked() },
                            enabled = !state.backupInProgress && !state.restoreInProgress,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Importieren")
                        }
                    }

                    Text(
                        text = "Importieren ersetzt deine lokalen Daten vollstaendig.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.backupLastSuccessEpochMs?.let { epochMs ->
                        Text(
                            text = "Letzter erfolgreicher Lauf: ${DateFormat.getDateTimeInstance().format(Date(epochMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    state.backupMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.backupStatus == BackupStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private suspend fun runCredentialManagerSignIn(context: Context, viewModel: SettingsViewModel) {
    val activity = context.findActivity()
    if (activity == null) {
        viewModel.onCredentialManagerSignInFailed("Google-Anmeldung ist auf diesem Bildschirm nicht verfuegbar.")
        return
    }

    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (webClientId.isBlank() || webClientId == "dev-change-me") {
        viewModel.onCredentialManagerSignInFailed("Google OAuth Client-ID fehlt. Bitte Build-Konfiguration pruefen.")
        return
    }

    val credentialManager = CredentialManager.create(context)
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
        )
        .build()

    try {
        val result = credentialManager.getCredential(activity, request)
        val email = extractEmailFromCredential(result.credential)
        if (email.isNullOrBlank()) {
            viewModel.onCredentialManagerSignInFailed("Google-Konto konnte nicht gelesen werden.")
        } else {
            viewModel.onCredentialManagerSignInSuccess(email)
        }
    } catch (_: GetCredentialCancellationException) {
        viewModel.onCredentialManagerSignInFailed("Google-Anmeldung wurde abgebrochen.")
    } catch (_: NoCredentialException) {
        viewModel.onCredentialManagerSignInFailed("Kein Google-Konto verfuegbar.")
    } catch (e: GetCredentialException) {
        viewModel.onCredentialManagerSignInFailed(e.message ?: "Google-Anmeldung fehlgeschlagen.")
    } catch (_: Throwable) {
        viewModel.onCredentialManagerSignInFailed("Google-Anmeldung konnte nicht verarbeitet werden.")
    }
}

private fun extractEmailFromCredential(credential: androidx.credentials.Credential): String? {
    if (credential !is CustomCredential) return null
    if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) return null
    val tokenCredential = try {
        GoogleIdTokenCredential.createFrom(credential.data)
    } catch (_: GoogleIdTokenParsingException) {
        return null
    }
    return extractEmailFromIdToken(tokenCredential.idToken)
}

private fun extractEmailFromIdToken(idToken: String): String? {
    val parts = idToken.split('.')
    if (parts.size < 2) return null
    return runCatching {
        val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val payloadJson = JSONObject(String(payloadBytes))
        if (payloadJson.has("email")) payloadJson.getString("email") else null
    }.getOrNull()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    }
    Row(
        modifier = rowModifier,
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
        trailing?.invoke()
    }
}

private fun AppThemeMode.displayLabel(): String = when (this) {
    AppThemeMode.SYSTEM -> "System"
    AppThemeMode.LIGHT -> "Hell"
    AppThemeMode.DARK -> "Dunkel"
}

enum class SettingsSection(val title: String) {
    MAIN("Einstellungen"),
    REMINDER("Erinnerung"),
    BACKUP("Backup & Wiederherstellung")
}

private fun formatReminderSummary(enabled: Boolean, hour: Int, minute: Int): String {
    return if (enabled) {
        "Aktiv ${formatTimeLabel(hour, minute)}"
    } else {
        "Deaktiviert"
    }
}

private fun formatBackupSummary(state: SettingsUiState): String {
    return when {
        state.backupInProgress -> "Backup laeuft"
        state.restoreInProgress -> "Wiederherstellung laeuft"
        !state.backupConnectedAccount.isNullOrBlank() -> state.backupConnectedAccount
        else -> "Nicht verbunden"
    }
}

private fun formatTimeLabel(hour: Int, minute: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

private fun UpdateStatus.label(): String = when (this) {
    UpdateStatus.IDLE -> "Noch nicht geprueft"
    UpdateStatus.CHECKING -> "Pruefung laeuft"
    UpdateStatus.UP_TO_DATE -> "App ist aktuell"
    UpdateStatus.UPDATE_AVAILABLE -> "Update verfuegbar"
    UpdateStatus.ERROR -> "Fehler bei der Update-Pruefung"
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        if (it is ActivityNotFoundException) return
    }
}
