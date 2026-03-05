package de.ingomc.nozio.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.ingomc.nozio.NozioApplication

class WeeklyBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NozioApplication ?: return Result.failure()
        return runBackupOnce(app.driveBackupService, app.backupRepository)
    }

    companion object {
        internal suspend fun runBackupOnce(
            driveService: DriveBackupService,
            backupRepository: BackupRepository
        ): Result {
            if (!driveService.isSignedIn()) return Result.success()
            val json = runCatching { backupRepository.createBackupJson() }
                .getOrElse { return Result.retry() }
            return when (driveService.uploadBackup(json)) {
                is UploadResult.Success -> Result.success()
                is UploadResult.Error -> Result.retry()
            }
        }
    }
}
