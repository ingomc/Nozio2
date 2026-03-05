package de.ingomc.nozio.data.backup

import android.content.Context
import android.content.Intent
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalFileBackupService(
    appContext: Context
) : DriveBackupService {

    private val compressedBackupFile = File(appContext.filesDir, COMPRESSED_BACKUP_FILE_NAME)
    private val legacyBackupFile = File(appContext.filesDir, LEGACY_BACKUP_FILE_NAME)

    override suspend fun ensureAuthorized(): DriveAuthState = DriveAuthState.Ready(LOCAL_ACCOUNT_LABEL)

    override suspend fun completeAuthorization(resultData: Intent?): DriveAuthState = DriveAuthState.Ready(LOCAL_ACCOUNT_LABEL)

    override fun setSignedInAccountEmail(accountEmail: String?) = Unit

    override fun clearSignedInAccount() = Unit

    override suspend fun uploadBackup(json: String): UploadResult = withContext(Dispatchers.IO) {
        runCatching {
            compressedBackupFile.parentFile?.mkdirs()
            GZIPOutputStream(compressedBackupFile.outputStream()).bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(json)
            }
            if (legacyBackupFile.exists()) {
                legacyBackupFile.delete()
            }
        }.fold(
            onSuccess = { UploadResult.Success(System.currentTimeMillis()) },
            onFailure = { UploadResult.Error("Backup-Datei konnte nicht gespeichert werden.") }
        )
    }

    override suspend fun downloadLatestBackup(): DownloadResult = withContext(Dispatchers.IO) {
        val sourceFile = when {
            compressedBackupFile.exists() -> compressedBackupFile
            legacyBackupFile.exists() -> legacyBackupFile
            else -> return@withContext DownloadResult.NotFound
        }

        runCatching {
            if (sourceFile == compressedBackupFile) {
                GZIPInputStream(sourceFile.inputStream()).bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            } else {
                sourceFile.readText()
            }
        }.fold(
            onSuccess = { DownloadResult.Success(it) },
            onFailure = { DownloadResult.Error("Backup-Datei konnte nicht gelesen werden.") }
        )
    }

    override fun isSignedIn(): Boolean = true

    override fun currentAccountEmail(): String? = LOCAL_ACCOUNT_LABEL

    companion object {
        private const val COMPRESSED_BACKUP_FILE_NAME = "nozio-backup-v1.json.gz"
        private const val LEGACY_BACKUP_FILE_NAME = "nozio-backup-v1.json"
        private const val LOCAL_ACCOUNT_LABEL = "Lokaler Speicher"
    }
}
