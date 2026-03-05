package de.ingomc.nozio.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BackupScheduler(
    private val context: Context
) {
    fun scheduleWeeklyBackup() {
        val request = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelWeeklyBackup() {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_BACKUP_WORK_NAME)
    }

    companion object {
        const val WEEKLY_BACKUP_WORK_NAME = "weekly_google_drive_backup"
    }
}
