package de.ingomc.nozio.data.backup

import android.content.Intent
import android.content.IntentSender

enum class DriveAuthReasonCode {
    SIGNED_OUT,
    USER_CANCELED,
    AUTHORIZATION_DENIED,
    NETWORK,
    CONFIGURATION,
    UNKNOWN
}

sealed interface DriveAuthState {
    data object SignedOut : DriveAuthState
    data object NeedsSignIn : DriveAuthState
    data class NeedsDriveAuthorization(val intentSender: IntentSender) : DriveAuthState
    data class Ready(val accountEmail: String?) : DriveAuthState
    data class Error(
        val message: String,
        val reasonCode: DriveAuthReasonCode = DriveAuthReasonCode.UNKNOWN
    ) : DriveAuthState
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
    suspend fun ensureAuthorized(): DriveAuthState
    suspend fun completeAuthorization(resultData: Intent?): DriveAuthState
    fun setSignedInAccountEmail(accountEmail: String?)
    fun clearSignedInAccount()
    suspend fun uploadBackup(json: String): UploadResult
    suspend fun downloadLatestBackup(): DownloadResult
    fun isSignedIn(): Boolean
    fun currentAccountEmail(): String?
}
