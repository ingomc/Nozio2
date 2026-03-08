package de.ingomc.nozio.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.backup.BackupRepository
import de.ingomc.nozio.data.backup.BackupDocumentService
import de.ingomc.nozio.data.backup.DownloadResult
import de.ingomc.nozio.data.backup.DriveAuthState
import de.ingomc.nozio.data.backup.DriveBackupService
import de.ingomc.nozio.data.backup.RestoreResult
import de.ingomc.nozio.data.backup.UploadResult
import de.ingomc.nozio.data.repository.AppThemeMode
import de.ingomc.nozio.data.repository.AppUpdateChecker
import de.ingomc.nozio.data.repository.AvailableRelease
import de.ingomc.nozio.data.repository.UpdateCheckResult
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.update.UpdateInstaller
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    ERROR
}

enum class BackupStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR
}

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val mealReminderEnabled: Boolean = false,
    val mealReminderHour: Int = 19,
    val mealReminderMinute: Int = 0,
    val appVersionLabel: String = "",
    val updateStatus: UpdateStatus = UpdateStatus.IDLE,
    val availableReleaseTitle: String? = null,
    val availableReleaseNotesPreview: String? = null,
    val downloadInProgress: Boolean = false,
    val hasDownloadableUpdate: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val errorMessage: String? = null,
    val autoBackupEnabled: Boolean = true,
    val backupStatus: BackupStatus = BackupStatus.IDLE,
    val backupConnectedAccount: String? = null,
    val backupLastSuccessEpochMs: Long? = null,
    val backupMessage: String? = null,
    val backupInProgress: Boolean = false,
    val restoreInProgress: Boolean = false,
    val showRestoreConfirmation: Boolean = false
)

sealed interface SettingsEffect {
    data class OpenUrl(val url: String) : SettingsEffect
    data object OpenUnknownSourcesSettings : SettingsEffect
    data class StartInstall(val intent: Intent) : SettingsEffect
    data object LaunchCredentialManagerSignIn : SettingsEffect
    data class LaunchDriveAuthorization(val intentSender: IntentSender) : SettingsEffect
    data class LaunchBackupExport(val suggestedFileName: String) : SettingsEffect
    data object LaunchBackupImport : SettingsEffect
}

interface UserPreferencesStore {
    val userPreferences: Flow<UserPreferences>
    suspend fun updatePreferences(preferences: UserPreferences)
}

class RepositoryUserPreferencesStore(
    private val userPreferencesRepository: UserPreferencesRepository
) : UserPreferencesStore {
    override val userPreferences: Flow<UserPreferences> = userPreferencesRepository.userPreferences

    override suspend fun updatePreferences(preferences: UserPreferences) {
        userPreferencesRepository.updatePreferences(preferences)
    }
}

    private enum class PendingDriveAction {
    NONE,
    BACKUP,
    RESTORE
}

class SettingsViewModel(
    private val userPreferencesStore: UserPreferencesStore,
    private val onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    private val onAutoBackupChanged: (enabled: Boolean) -> Unit,
    private val appUpdateChecker: AppUpdateChecker,
    private val updateInstaller: UpdateInstaller,
    private val driveBackupService: DriveBackupService,
    private val backupRepository: BackupRepository,
    private val backupDocumentService: BackupDocumentService,
    private val appVersionName: String,
    private val appVersionCode: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            appVersionLabel = "Version $appVersionName ($appVersionCode)",
            backupConnectedAccount = driveBackupService.currentAccountEmail()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    private var hasAutoCheckedForUpdates = false
    private var pendingDownloadId: Long? = null
    private var latestAvailableRelease: AvailableRelease? = null
    private var pendingDriveAction: PendingDriveAction = PendingDriveAction.NONE
    private var pendingExportJson: String? = null

    init {
        viewModelScope.launch {
            userPreferencesStore.userPreferences.collectLatest { prefs ->
                _uiState.update { current ->
                    current.copy(
                        themeMode = prefs.themeMode,
                        autoBackupEnabled = prefs.autoBackupEnabled,
                        mealReminderEnabled = prefs.mealReminderEnabled,
                        mealReminderHour = prefs.mealReminderHour,
                        mealReminderMinute = prefs.mealReminderMinute
                    )
                }
            }
        }
    }

    fun onSettingsOpened() {
        if (hasAutoCheckedForUpdates) return
        hasAutoCheckedForUpdates = true
        checkForUpdate()
    }

    fun onCheckForUpdateClicked() {
        checkForUpdate()
    }

    fun onThemeModeChange(mode: AppThemeMode) {
        viewModelScope.launch {
            val currentPrefs = userPreferencesStore.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.themeMode == mode) return@launch
            userPreferencesStore.updatePreferences(currentPrefs.copy(themeMode = mode))
        }
    }

    fun onMealReminderEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = userPreferencesStore.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.mealReminderEnabled == enabled) return@launch
            userPreferencesStore.updatePreferences(currentPrefs.copy(mealReminderEnabled = enabled))
            onReminderChanged(enabled, currentPrefs.mealReminderHour, currentPrefs.mealReminderMinute)
        }
    }

    fun onAutoBackupEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = userPreferencesStore.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.autoBackupEnabled == enabled) return@launch
            userPreferencesStore.updatePreferences(currentPrefs.copy(autoBackupEnabled = enabled))
            onAutoBackupChanged(enabled)
            if (!enabled) {
                pendingDriveAction = PendingDriveAction.NONE
                _uiState.update {
                    it.copy(showRestoreConfirmation = false)
                }
            }
        }
    }

    fun onMealReminderTimeChange(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        viewModelScope.launch {
            val currentPrefs = userPreferencesStore.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.mealReminderHour == safeHour && currentPrefs.mealReminderMinute == safeMinute) {
                return@launch
            }
            userPreferencesStore.updatePreferences(
                currentPrefs.copy(
                    mealReminderHour = safeHour,
                    mealReminderMinute = safeMinute
                )
            )
            onReminderChanged(currentPrefs.mealReminderEnabled, safeHour, safeMinute)
        }
    }

    fun onDriveSignInClicked() {
        pendingDriveAction = PendingDriveAction.NONE
        ensureDriveReadyOrRequestSignIn()
    }

    fun onCredentialManagerSignInSuccess(accountEmail: String?) {
        driveBackupService.setSignedInAccountEmail(accountEmail)
        ensureDriveReadyOrRequestSignIn()
    }

    fun onCredentialManagerSignInFailed(message: String) {
        pendingDriveAction = PendingDriveAction.NONE
        _uiState.update {
            it.copy(
                backupStatus = BackupStatus.ERROR,
                backupMessage = message
            )
        }
    }

    fun onDriveAuthorizationResult(resultCode: Int, resultData: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            pendingDriveAction = PendingDriveAction.NONE
            _uiState.update {
                it.copy(
                    backupStatus = BackupStatus.ERROR,
                    backupMessage = "Vorgang wurde abgebrochen."
                )
            }
            return
        }
        viewModelScope.launch {
            handleAuthState(driveBackupService.completeAuthorization(resultData))
        }
    }

    fun onBackupNowClicked() {
        if (uiState.value.backupInProgress || uiState.value.restoreInProgress) return
        pendingDriveAction = PendingDriveAction.BACKUP
        ensureDriveReadyOrRequestSignIn()
    }

    fun onRestoreClicked() {
        if (uiState.value.backupInProgress || uiState.value.restoreInProgress) return
        _uiState.update { it.copy(showRestoreConfirmation = true) }
    }

    fun onRestoreDialogDismissed() {
        _uiState.update { it.copy(showRestoreConfirmation = false) }
    }

    fun onRestoreConfirmed() {
        if (uiState.value.backupInProgress || uiState.value.restoreInProgress) return
        _uiState.update { it.copy(showRestoreConfirmation = false) }
        pendingDriveAction = PendingDriveAction.RESTORE
        ensureDriveReadyOrRequestSignIn()
    }

    fun onExportBackupClicked() {
        if (uiState.value.backupInProgress || uiState.value.restoreInProgress) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    backupInProgress = true,
                    backupStatus = BackupStatus.RUNNING,
                    backupMessage = "Backup-Datei wird vorbereitet..."
                )
            }
            val json = runCatching { backupRepository.createBackupJson() }
                .getOrElse {
                    pendingExportJson = null
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            backupStatus = BackupStatus.ERROR,
                            backupMessage = "Backup-Export fehlgeschlagen."
                        )
                    }
                    return@launch
                }
            pendingExportJson = json
            emitEffect(SettingsEffect.LaunchBackupExport("nozio-backup-v1.json.gz"))
        }
    }

    fun onBackupExportDestinationSelected(uri: Uri?) {
        if (uri == null) {
            pendingExportJson = null
            _uiState.update {
                it.copy(
                    backupInProgress = false,
                    backupStatus = BackupStatus.ERROR,
                    backupMessage = "Backup-Export wurde abgebrochen."
                )
            }
            return
        }
        val exportJson = pendingExportJson
        if (exportJson == null) {
            _uiState.update {
                it.copy(
                    backupInProgress = false,
                    backupStatus = BackupStatus.ERROR,
                    backupMessage = "Keine Backup-Daten zum Export vorhanden."
                )
            }
            return
        }
        viewModelScope.launch {
            val result = backupDocumentService.exportToUri(uri, exportJson)
            pendingExportJson = null
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            backupStatus = BackupStatus.SUCCESS,
                            backupLastSuccessEpochMs = System.currentTimeMillis(),
                            backupMessage = "Backup-Datei exportiert."
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            backupInProgress = false,
                            backupStatus = BackupStatus.ERROR,
                            backupMessage = "Backup-Datei konnte nicht exportiert werden."
                        )
                    }
                }
            )
        }
    }

    fun onImportBackupClicked() {
        if (uiState.value.backupInProgress || uiState.value.restoreInProgress) return
        emitEffect(SettingsEffect.LaunchBackupImport)
    }

    fun onBackupImportSourceSelected(uri: Uri?) {
        if (uri == null) {
            _uiState.update {
                it.copy(
                    backupStatus = BackupStatus.ERROR,
                    backupMessage = "Backup-Import wurde abgebrochen."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    restoreInProgress = true,
                    backupStatus = BackupStatus.RUNNING,
                    backupMessage = "Backup-Datei wird importiert..."
                )
            }
            val content = backupDocumentService.importFromUri(uri).getOrElse {
                _uiState.update {
                    it.copy(
                        restoreInProgress = false,
                        backupStatus = BackupStatus.ERROR,
                        backupMessage = "Backup-Datei konnte nicht gelesen werden."
                    )
                }
                return@launch
            }
            when (val restore = backupRepository.restoreFromBackupJson(content)) {
                is RestoreResult.Success -> {
                    _uiState.update {
                        it.copy(
                            restoreInProgress = false,
                            backupStatus = BackupStatus.SUCCESS,
                            backupLastSuccessEpochMs = restore.restoredFromEpochMs,
                            backupMessage = "Import erfolgreich (${restore.foodCount} Lebensmittel, ${restore.diaryEntryCount} Eintraege)."
                        )
                    }
                }

                is RestoreResult.Error -> {
                    _uiState.update {
                        it.copy(
                            restoreInProgress = false,
                            backupStatus = BackupStatus.ERROR,
                            backupMessage = restore.message
                        )
                    }
                }
            }
        }
    }

    private fun ensureDriveReadyOrRequestSignIn() {
        viewModelScope.launch {
            handleAuthState(driveBackupService.ensureAuthorized())
        }
    }

    private suspend fun handleAuthState(state: DriveAuthState) {
        when (state) {
            is DriveAuthState.Ready -> {
                val accountLabel = state.accountEmail ?: "Backup-Speicher bereit"
                _uiState.update {
                    it.copy(
                        backupConnectedAccount = accountLabel,
                        backupStatus = BackupStatus.SUCCESS,
                        backupMessage = "Backup-Speicher bereit."
                    )
                }
                when (pendingDriveAction) {
                    PendingDriveAction.BACKUP -> runBackupNow()
                    PendingDriveAction.RESTORE -> runRestoreNow()
                    PendingDriveAction.NONE -> Unit
                }
                pendingDriveAction = PendingDriveAction.NONE
            }

            is DriveAuthState.NeedsSignIn -> {
                emitEffect(SettingsEffect.LaunchCredentialManagerSignIn)
            }

            is DriveAuthState.NeedsDriveAuthorization -> {
                emitEffect(SettingsEffect.LaunchDriveAuthorization(state.intentSender))
            }

            is DriveAuthState.SignedOut -> {
                pendingDriveAction = PendingDriveAction.NONE
                driveBackupService.clearSignedInAccount()
                _uiState.update {
                    it.copy(
                        backupConnectedAccount = null,
                        backupStatus = BackupStatus.ERROR,
                        backupMessage = "Backup-Speicher ist nicht bereit."
                    )
                }
            }

            is DriveAuthState.Error -> {
                pendingDriveAction = PendingDriveAction.NONE
                _uiState.update {
                    it.copy(
                        backupStatus = BackupStatus.ERROR,
                        backupMessage = state.message
                    )
                }
            }
        }
    }

    private suspend fun runBackupNow() {
        _uiState.update {
            it.copy(
                backupInProgress = true,
                backupStatus = BackupStatus.RUNNING,
                backupMessage = "Backup wird erstellt..."
            )
        }

        val backupJson = runCatching { backupRepository.createBackupJson() }
            .getOrElse {
                _uiState.update {
                    it.copy(
                        backupInProgress = false,
                        backupStatus = BackupStatus.ERROR,
                        backupMessage = "Backup-Erstellung fehlgeschlagen."
                    )
                }
                return
            }

        when (val upload = driveBackupService.uploadBackup(backupJson)) {
            is UploadResult.Success -> {
                _uiState.update {
                    it.copy(
                        backupInProgress = false,
                        backupStatus = BackupStatus.SUCCESS,
                        backupLastSuccessEpochMs = upload.modifiedAtEpochMs,
                        backupConnectedAccount = driveBackupService.currentAccountEmail(),
                        backupMessage = "Backup erfolgreich gespeichert."
                    )
                }
            }

            is UploadResult.Error -> {
                _uiState.update {
                    it.copy(
                        backupInProgress = false,
                        backupStatus = BackupStatus.ERROR,
                        backupMessage = upload.message
                    )
                }
            }
        }
    }

    private suspend fun runRestoreNow() {
        _uiState.update {
            it.copy(
                restoreInProgress = true,
                backupStatus = BackupStatus.RUNNING,
                backupMessage = "Backup wird wiederhergestellt..."
            )
        }

        when (val download = driveBackupService.downloadLatestBackup()) {
            is DownloadResult.NotFound -> {
                _uiState.update {
                    it.copy(
                        restoreInProgress = false,
                        backupStatus = BackupStatus.ERROR,
                        backupMessage = "Keine lokale Backup-Datei gefunden."
                    )
                }
            }

            is DownloadResult.Error -> {
                _uiState.update {
                    it.copy(
                        restoreInProgress = false,
                        backupStatus = BackupStatus.ERROR,
                        backupMessage = download.message
                    )
                }
            }

            is DownloadResult.Success -> {
                when (val restore = backupRepository.restoreFromBackupJson(download.content)) {
                    is RestoreResult.Success -> {
                        _uiState.update {
                            it.copy(
                                restoreInProgress = false,
                                backupStatus = BackupStatus.SUCCESS,
                                backupLastSuccessEpochMs = restore.restoredFromEpochMs,
                                backupConnectedAccount = driveBackupService.currentAccountEmail(),
                                backupMessage = "Wiederherstellung erfolgreich (${restore.foodCount} Lebensmittel, ${restore.diaryEntryCount} Eintraege)."
                            )
                        }
                    }

                    is RestoreResult.Error -> {
                        _uiState.update {
                            it.copy(
                                restoreInProgress = false,
                                backupStatus = BackupStatus.ERROR,
                                backupMessage = restore.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun onUpdateDialogDismissed() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun onOpenReleasePageClicked() {
        val releaseUrl = latestAvailableRelease?.htmlUrl ?: return
        emitEffect(SettingsEffect.OpenUrl(releaseUrl))
    }

    fun onDownloadUpdateClicked() {
        val release = latestAvailableRelease ?: return
        val apkAsset = release.apkAsset
        if (apkAsset == null) {
            emitEffect(SettingsEffect.OpenUrl(release.htmlUrl))
            return
        }

        viewModelScope.launch {
            runCatching {
                updateInstaller.enqueueDownload(url = apkAsset.downloadUrl, fileName = apkAsset.name)
            }.onSuccess { downloadId ->
                pendingDownloadId = downloadId
                _uiState.update {
                    it.copy(
                        downloadInProgress = true,
                        errorMessage = null
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        updateStatus = UpdateStatus.ERROR,
                        errorMessage = "Update konnte nicht heruntergeladen werden.",
                        downloadInProgress = false
                    )
                }
                emitEffect(SettingsEffect.OpenUrl(release.htmlUrl))
            }
        }
    }

    fun onDownloadCompleted(downloadId: Long) {
        val expectedDownloadId = pendingDownloadId ?: return
        if (downloadId != expectedDownloadId) return

        _uiState.update { it.copy(downloadInProgress = false) }
        onInstallDownloadedUpdateClicked()
    }

    fun onInstallDownloadedUpdateClicked() {
        val downloadId = pendingDownloadId ?: return
        if (!updateInstaller.canInstallUnknownApps()) {
            emitEffect(SettingsEffect.OpenUnknownSourcesSettings)
            return
        }

        val installIntent = updateInstaller.buildInstallIntent(downloadId)
        if (installIntent == null) {
            _uiState.update {
                it.copy(
                    updateStatus = UpdateStatus.ERROR,
                    errorMessage = "Heruntergeladene APK konnte nicht geoeffnet werden."
                )
            }
            latestAvailableRelease?.htmlUrl?.let { emitEffect(SettingsEffect.OpenUrl(it)) }
            return
        }

        emitEffect(SettingsEffect.StartInstall(installIntent))
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updateStatus = UpdateStatus.CHECKING,
                    errorMessage = null
                )
            }

            runCatching {
                appUpdateChecker.checkForUpdate(currentVersionName = appVersionName)
            }.onSuccess { result ->
                when (result) {
                    UpdateCheckResult.UpToDate -> {
                        latestAvailableRelease = null
                        pendingDownloadId = null
                        _uiState.update {
                            it.copy(
                                updateStatus = UpdateStatus.UP_TO_DATE,
                                availableReleaseTitle = null,
                                availableReleaseNotesPreview = null,
                                hasDownloadableUpdate = false,
                                showUpdateDialog = false,
                                downloadInProgress = false,
                                errorMessage = null
                            )
                        }
                    }

                    is UpdateCheckResult.UpdateAvailable -> {
                        latestAvailableRelease = result.release
                        pendingDownloadId = null
                        _uiState.update {
                            it.copy(
                                updateStatus = UpdateStatus.UPDATE_AVAILABLE,
                                availableReleaseTitle = result.release.title,
                                availableReleaseNotesPreview = result.release.notesPreview,
                                hasDownloadableUpdate = result.release.apkAsset != null,
                                showUpdateDialog = true,
                                downloadInProgress = false,
                                errorMessage = null
                            )
                        }
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        updateStatus = UpdateStatus.ERROR,
                        errorMessage = "Update-Pruefung fehlgeschlagen."
                    )
                }
            }
        }
    }

    private fun emitEffect(effect: SettingsEffect) {
        _effects.tryEmit(effect)
    }

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val appUpdateChecker: AppUpdateChecker,
        private val updateInstaller: UpdateInstaller,
        private val driveBackupService: DriveBackupService,
        private val backupRepository: BackupRepository,
        private val backupDocumentService: BackupDocumentService,
        private val appVersionName: String,
        private val appVersionCode: Int,
        private val onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
        private val onAutoBackupChanged: (enabled: Boolean) -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                userPreferencesStore = RepositoryUserPreferencesStore(userPreferencesRepository),
                onReminderChanged = onReminderChanged,
                onAutoBackupChanged = onAutoBackupChanged,
                appUpdateChecker = appUpdateChecker,
                updateInstaller = updateInstaller,
                driveBackupService = driveBackupService,
                backupRepository = backupRepository,
                backupDocumentService = backupDocumentService,
                appVersionName = appVersionName,
                appVersionCode = appVersionCode
            ) as T
        }
    }
}
