package de.ingomc.nozio.ui.settings

import android.content.Intent
import de.ingomc.nozio.data.backup.BackupRepository
import de.ingomc.nozio.data.backup.DownloadResult
import de.ingomc.nozio.data.backup.DriveBackupService
import de.ingomc.nozio.data.backup.RestoreResult
import de.ingomc.nozio.data.backup.SignInResult
import de.ingomc.nozio.data.backup.UploadResult
import de.ingomc.nozio.data.repository.ApkAssetSelection
import de.ingomc.nozio.data.repository.AppUpdateChecker
import de.ingomc.nozio.data.repository.AvailableRelease
import de.ingomc.nozio.data.repository.UpdateCheckResult
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.update.UpdateInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onSettingsOpened_runsAutoCheckOnlyOnce() = runTest(dispatcher) {
        val checker = FakeAppUpdateChecker(UpdateCheckResult.UpToDate)
        val viewModel = createViewModel(checker = checker)

        viewModel.onSettingsOpened()
        viewModel.onSettingsOpened()
        advanceUntilIdle()

        assertEquals(1, checker.calls)
        assertEquals(UpdateStatus.UP_TO_DATE, viewModel.uiState.value.updateStatus)
    }

    @Test
    fun onCheckForUpdateClicked_setsUpdateAvailableState() = runTest(dispatcher) {
        val checker = FakeAppUpdateChecker(
            UpdateCheckResult.UpdateAvailable(
                AvailableRelease(
                    tagName = "v0.5.2",
                    title = "Nozio 0.5.2",
                    notesPreview = "Fixes and improvements",
                    htmlUrl = "https://github.com/ingomc/Nozio2/releases/tag/v0.5.2",
                    apkAsset = ApkAssetSelection(
                        name = "app-release-universal.apk",
                        downloadUrl = "https://example.com/app-release-universal.apk"
                    )
                )
            )
        )
        val viewModel = createViewModel(checker = checker)

        viewModel.onCheckForUpdateClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UpdateStatus.UPDATE_AVAILABLE, state.updateStatus)
        assertEquals("Nozio 0.5.2", state.availableReleaseTitle)
        assertEquals("Fixes and improvements", state.availableReleaseNotesPreview)
        assertTrue(state.hasDownloadableUpdate)
        assertTrue(state.showUpdateDialog)
    }

    @Test
    fun onCheckForUpdateClicked_setsUpToDateState() = runTest(dispatcher) {
        val checker = FakeAppUpdateChecker(UpdateCheckResult.UpToDate)
        val viewModel = createViewModel(checker = checker)

        viewModel.onCheckForUpdateClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UpdateStatus.UP_TO_DATE, state.updateStatus)
        assertEquals(null, state.availableReleaseTitle)
        assertEquals(false, state.showUpdateDialog)
    }

    @Test
    fun onCheckForUpdateClicked_setsErrorStateOnFailure() = runTest(dispatcher) {
        val checker = FakeAppUpdateChecker(error = IllegalStateException("boom"))
        val viewModel = createViewModel(checker = checker)

        viewModel.onCheckForUpdateClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UpdateStatus.ERROR, state.updateStatus)
        assertEquals("Update-Pruefung fehlgeschlagen.", state.errorMessage)
    }

    @Test
    fun onBackupNowClicked_setsSuccessState() = runTest(dispatcher) {
        val drive = FakeDriveBackupService(
            ensureResult = SignInResult.SignedIn("user@example.com"),
            uploadResult = UploadResult.Success(1710000000000L)
        )
        val backup = FakeBackupRepository(createJson = "{}")
        val viewModel = createViewModel(drive = drive, backupRepository = backup)

        viewModel.onBackupNowClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BackupStatus.SUCCESS, state.backupStatus)
        assertEquals("user@example.com", state.backupConnectedAccount)
        assertEquals(1710000000000L, state.backupLastSuccessEpochMs)
    }

    @Test
    fun onRestoreConfirmed_withMissingBackup_setsError() = runTest(dispatcher) {
        val drive = FakeDriveBackupService(
            ensureResult = SignInResult.SignedIn("user@example.com"),
            downloadResult = DownloadResult.NotFound
        )
        val viewModel = createViewModel(drive = drive)

        viewModel.onRestoreConfirmed()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BackupStatus.ERROR, state.backupStatus)
        assertEquals("Kein Backup in Google Drive gefunden.", state.backupMessage)
    }

    @Test
    fun onAutoBackupEnabledChange_updatesStateAndCallsCallback() = runTest(dispatcher) {
        val callbackValues = mutableListOf<Boolean>()
        val viewModel = createViewModel(
            userPreferencesStore = FakeUserPreferencesStore(
                initial = UserPreferences(autoBackupEnabled = true)
            ),
            onAutoBackupChanged = { enabled ->
                callbackValues += enabled
            }
        )

        viewModel.onAutoBackupEnabledChange(false)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.autoBackupEnabled)
        assertEquals(listOf(false), callbackValues)
    }

    @Test
    fun onBackupNowClicked_worksEvenWhenAutoBackupDisabled() = runTest(dispatcher) {
        val drive = FakeDriveBackupService(
            ensureResult = SignInResult.SignedIn("user@example.com"),
            uploadResult = UploadResult.Success(1710000000000L)
        )
        val backup = FakeBackupRepository(createJson = "{}")
        val viewModel = createViewModel(
            drive = drive,
            backupRepository = backup,
            userPreferencesStore = FakeUserPreferencesStore(
                initial = UserPreferences(autoBackupEnabled = false)
            )
        )

        viewModel.onBackupNowClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BackupStatus.SUCCESS, state.backupStatus)
        assertEquals(1710000000000L, state.backupLastSuccessEpochMs)
    }

    private fun createViewModel(
        checker: AppUpdateChecker = FakeAppUpdateChecker(UpdateCheckResult.UpToDate),
        drive: DriveBackupService = FakeDriveBackupService(),
        backupRepository: BackupRepository = FakeBackupRepository(),
        userPreferencesStore: UserPreferencesStore = FakeUserPreferencesStore(),
        onAutoBackupChanged: (Boolean) -> Unit = {}
    ): SettingsViewModel {
        return SettingsViewModel(
            userPreferencesStore = userPreferencesStore,
            onReminderChanged = { _, _, _ -> },
            onAutoBackupChanged = onAutoBackupChanged,
            appUpdateChecker = checker,
            updateInstaller = FakeUpdateInstaller(),
            driveBackupService = drive,
            backupRepository = backupRepository,
            appVersionName = "0.5.1",
            appVersionCode = 6
        )
    }

    private class FakeUserPreferencesStore(
        initial: UserPreferences = UserPreferences()
    ) : UserPreferencesStore {
        private val state = MutableStateFlow(initial)

        override val userPreferences: Flow<UserPreferences> = state

        override suspend fun updatePreferences(preferences: UserPreferences) {
            state.value = preferences
        }
    }

    private class FakeAppUpdateChecker(
        private val result: UpdateCheckResult? = null,
        private val error: Throwable? = null
    ) : AppUpdateChecker {
        var calls: Int = 0
            private set

        override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
            calls += 1
            error?.let { throw it }
            return result ?: UpdateCheckResult.UpToDate
        }
    }

    private class FakeUpdateInstaller : UpdateInstaller {
        override fun enqueueDownload(url: String, fileName: String): Long = 1L

        override fun buildInstallIntent(downloadId: Long): Intent? = Intent()

        override fun canInstallUnknownApps(): Boolean = true
    }

    private class FakeDriveBackupService(
        private val ensureResult: SignInResult = SignInResult.SignedIn("user@example.com"),
        private val uploadResult: UploadResult = UploadResult.Success(System.currentTimeMillis()),
        private val downloadResult: DownloadResult = DownloadResult.Success("{}")
    ) : DriveBackupService {
        override suspend fun ensureSignedIn(): SignInResult = ensureResult

        override suspend fun completeSignIn(signInResultData: Intent?): SignInResult = ensureResult

        override suspend fun uploadBackup(json: String): UploadResult = uploadResult

        override suspend fun downloadLatestBackup(): DownloadResult = downloadResult

        override fun isSignedIn(): Boolean = true

        override fun currentAccountEmail(): String? = "user@example.com"
    }

    private class FakeBackupRepository(
        private val createJson: String = "{}",
        private val restoreResult: RestoreResult = RestoreResult.Success(0, 0, 0, System.currentTimeMillis())
    ) : BackupRepository {
        override suspend fun createBackupJson(): String = createJson

        override suspend fun restoreFromBackupJson(json: String): RestoreResult = restoreResult
    }
}
