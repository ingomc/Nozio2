package de.ingomc.nozio.data.backup

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface BackupDocumentService {
    suspend fun exportToUri(uri: Uri, backupJson: String): Result<Unit>
    suspend fun importFromUri(uri: Uri): Result<String>
}

class AndroidBackupDocumentService(
    private val appContext: Context
) : BackupDocumentService {
    override suspend fun exportToUri(uri: Uri, backupJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                GZIPOutputStream(outputStream).bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(backupJson)
                }
            } ?: error("output stream unavailable")
        }
    }

    override suspend fun importFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("input stream unavailable")
            if (isGzip(bytes)) {
                GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            } else {
                bytes.toString(Charsets.UTF_8)
            }
        }
    }

    private fun isGzip(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        return bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
    }
}
