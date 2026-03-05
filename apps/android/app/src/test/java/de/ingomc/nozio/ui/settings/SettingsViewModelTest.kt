package de.ingomc.nozio.ui.settings

import android.content.Intent
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

    private fun createViewModel(
        checker: AppUpdateChecker
    ): SettingsViewModel {
        return SettingsViewModel(
            userPreferencesStore = FakeUserPreferencesStore(),
            onReminderChanged = { _, _, _ -> },
            appUpdateChecker = checker,
            updateInstaller = FakeUpdateInstaller(),
            appVersionName = "0.5.1",
            appVersionCode = 6
        )
    }

    private class FakeUserPreferencesStore : UserPreferencesStore {
        private val state = MutableStateFlow(UserPreferences())

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
}
