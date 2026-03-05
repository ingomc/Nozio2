package de.ingomc.nozio.data.backup

import android.content.Intent
import androidx.work.ListenableWorker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyBackupWorkerTest {

    @Test
    fun runBackupOnce_withSignedInUser_uploadsBackup() = runTest {
        val drive = FakeDriveBackupService(
            signedIn = true,
            uploadResult = UploadResult.Success(1710000000000L)
        )
        val repository = FakeBackupRepository()

        val result = WeeklyBackupWorker.runBackupOnce(drive, repository)

        assertEquals(ListenableWorker.Result.success()::class.java, result::class.java)
    }

    @Test
    fun runBackupOnce_withoutSignIn_skipsWithoutError() = runTest {
        val drive = FakeDriveBackupService(signedIn = false)
        val repository = FakeBackupRepository()

        val result = WeeklyBackupWorker.runBackupOnce(drive, repository)

        assertEquals(ListenableWorker.Result.success()::class.java, result::class.java)
    }

    private class FakeBackupRepository : BackupRepository {
        override suspend fun createBackupJson(): String = "{}"

        override suspend fun restoreFromBackupJson(json: String): RestoreResult {
            return RestoreResult.Success(0, 0, 0, 0)
        }
    }

    private class FakeDriveBackupService(
        private val signedIn: Boolean = true,
        private val uploadResult: UploadResult = UploadResult.Success(0)
    ) : DriveBackupService {
        override suspend fun ensureSignedIn(): SignInResult = SignInResult.SignedIn("user@example.com")

        override suspend fun completeSignIn(signInResultData: Intent?): SignInResult = SignInResult.SignedIn("user@example.com")

        override suspend fun uploadBackup(json: String): UploadResult = uploadResult

        override suspend fun downloadLatestBackup(): DownloadResult = DownloadResult.NotFound

        override fun isSignedIn(): Boolean = signedIn

        override fun currentAccountEmail(): String? = "user@example.com"
    }
}
