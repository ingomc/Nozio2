package de.ingomc.nozio.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment

interface UpdateInstaller {
    fun enqueueDownload(url: String, fileName: String): Long
    fun buildInstallIntent(downloadId: Long): Intent?
    fun canInstallUnknownApps(): Boolean
}

class ApkUpdateInstaller(
    private val context: Context
) : UpdateInstaller {

    private val downloadManager: DownloadManager =
        context.getSystemService(DownloadManager::class.java)

    override fun enqueueDownload(url: String, fileName: String): Long {
        val safeFileName = if (fileName.lowercase().endsWith(".apk")) fileName else "$fileName.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Nozio Update")
            .setDescription(safeFileName)
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, safeFileName)

        return downloadManager.enqueue(request)
    }

    override fun buildInstallIntent(downloadId: Long): Intent? {
        val uri = getDownloadedFileUri(downloadId) ?: return null
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override fun canInstallUnknownApps(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    private fun getDownloadedFileUri(downloadId: Long): Uri? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return null
        return cursor.use { c ->
            if (!c.moveToFirst()) return@use null
            val status = c.getIntSafely(DownloadManager.COLUMN_STATUS) ?: return@use null
            if (status != DownloadManager.STATUS_SUCCESSFUL) return@use null
            downloadManager.getUriForDownloadedFile(downloadId)
        }
    }

    private fun Cursor.getIntSafely(columnName: String): Int? {
        val index = getColumnIndex(columnName)
        if (index < 0) return null
        return getInt(index)
    }

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
