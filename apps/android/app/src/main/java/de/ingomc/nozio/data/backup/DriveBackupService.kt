package de.ingomc.nozio.data.backup

import android.content.Intent

sealed interface SignInResult {
    data class SignedIn(val accountEmail: String?) : SignInResult
    data class RequiresUserAction(val intent: Intent) : SignInResult
    data class Error(val message: String) : SignInResult
}

sealed interface UploadResult {
    data class Success(val modifiedAtEpochMs: Long) : UploadResult
    data class Error(val message: String) : UploadResult
}

sealed interface DownloadResult {
    data class Success(val content: String) : DownloadResult
    data object NotFound : DownloadResult
    data class Error(val message: String) : DownloadResult
}

interface DriveBackupService {
    suspend fun ensureSignedIn(): SignInResult
    suspend fun completeSignIn(signInResultData: Intent?): SignInResult
    suspend fun uploadBackup(json: String): UploadResult
    suspend fun downloadLatestBackup(): DownloadResult
    fun isSignedIn(): Boolean
    fun currentAccountEmail(): String?
}
