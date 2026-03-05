package de.ingomc.nozio.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.repository.AppUpdateChecker
import de.ingomc.nozio.data.repository.AppThemeMode
import de.ingomc.nozio.data.repository.AvailableRelease
import de.ingomc.nozio.data.repository.UpdateCheckResult
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.update.UpdateInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val errorMessage: String? = null
)

sealed interface SettingsEffect {
    data class OpenUrl(val url: String) : SettingsEffect
    data object OpenUnknownSourcesSettings : SettingsEffect
    data class StartInstall(val intent: Intent) : SettingsEffect
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

class SettingsViewModel(
    private val userPreferencesStore: UserPreferencesStore,
    private val onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    private val appUpdateChecker: AppUpdateChecker,
    private val updateInstaller: UpdateInstaller,
    private val appVersionName: String,
    private val appVersionCode: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(appVersionLabel = "Version $appVersionName ($appVersionCode)")
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    private var hasAutoCheckedForUpdates = false
    private var pendingDownloadId: Long? = null
    private var latestAvailableRelease: AvailableRelease? = null

    init {
        viewModelScope.launch {
            userPreferencesStore.userPreferences.collectLatest { prefs ->
                _uiState.update { current ->
                    current.copy(
                        themeMode = prefs.themeMode,
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
        private val appVersionName: String,
        private val appVersionCode: Int,
        private val onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                userPreferencesStore = RepositoryUserPreferencesStore(userPreferencesRepository),
                onReminderChanged = onReminderChanged,
                appUpdateChecker = appUpdateChecker,
                updateInstaller = updateInstaller,
                appVersionName = appVersionName,
                appVersionCode = appVersionCode
            ) as T
        }
    }
}
