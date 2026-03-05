package de.ingomc.nozio.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveBackupService(
    private val appContext: Context
) : DriveBackupService {

    private val authorizationClient by lazy { Identity.getAuthorizationClient(appContext) }
    private val driveScope = Scope(DriveScopes.DRIVE_APPDATA)
    private val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
        appContext,
        listOf(DriveScopes.DRIVE_APPDATA)
    )

    private val authPrefs = appContext.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)

    override suspend fun ensureAuthorized(): DriveAuthState = withContext(Dispatchers.IO) {
        val accountEmail = currentAccountEmail()
            ?: return@withContext DriveAuthState.NeedsSignIn

        return@withContext try {
            val request = AuthorizationRequest.Builder()
                .setRequestedScopes(listOf(driveScope))
                .setAccount(Account(accountEmail, GOOGLE_ACCOUNT_TYPE))
                .build()
            val authResult = Tasks.await(authorizationClient.authorize(request))
            resolveAuthorizationResult(authResult, fallbackAccountEmail = accountEmail)
        } catch (e: ApiException) {
            mapApiExceptionToAuthError(e)
        } catch (_: Throwable) {
            DriveAuthState.Error("Google Drive Autorisierung fehlgeschlagen.")
        }
    }

    override suspend fun completeAuthorization(resultData: Intent?): DriveAuthState = withContext(Dispatchers.IO) {
        if (resultData == null) {
            return@withContext DriveAuthState.Error(
                message = "Google Drive Autorisierung wurde abgebrochen.",
                reasonCode = DriveAuthReasonCode.USER_CANCELED
            )
        }

        return@withContext try {
            val result = authorizationClient.getAuthorizationResultFromIntent(resultData)
            resolveAuthorizationResult(result, fallbackAccountEmail = currentAccountEmail())
        } catch (e: ApiException) {
            mapApiExceptionToAuthError(e)
        } catch (_: Throwable) {
            DriveAuthState.Error("Google Drive Autorisierung konnte nicht verarbeitet werden.")
        }
    }

    override fun setSignedInAccountEmail(accountEmail: String?) {
        val normalized = accountEmail?.trim().orEmpty()
        if (normalized.isBlank()) {
            authPrefs.edit().remove(KEY_ACCOUNT_EMAIL).apply()
            credential.selectedAccountName = null
            return
        }
        authPrefs.edit().putString(KEY_ACCOUNT_EMAIL, normalized).apply()
        credential.selectedAccountName = normalized
    }

    override fun clearSignedInAccount() {
        authPrefs.edit().remove(KEY_ACCOUNT_EMAIL).apply()
        credential.selectedAccountName = null
    }

    override suspend fun uploadBackup(json: String): UploadResult = withContext(Dispatchers.IO) {
        val drive = createDriveClient() ?: return@withContext UploadResult.Error("Kein Google-Konto verbunden.")
        val existingFile = runCatching {
            drive.files().list()
                .setSpaces(APP_DATA_SPACE)
                .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                .setFields("files(id)")
                .execute()
                .files
                ?.firstOrNull()
        }.getOrNull()

        val content = ByteArrayContent.fromString("application/json", json)
        val modifiedAt = runCatching {
            if (existingFile?.id != null) {
                drive.files().update(existingFile.id, null, content)
                    .setFields("modifiedTime")
                    .execute()
            } else {
                val metadata = File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf(APP_DATA_SPACE)
                }
                drive.files().create(metadata, content)
                    .setFields("modifiedTime")
                    .execute()
            }
        }.getOrElse {
            return@withContext UploadResult.Error("Upload zu Google Drive fehlgeschlagen.")
        }

        UploadResult.Success(
            modifiedAtEpochMs = modifiedAt.modifiedTime?.value ?: System.currentTimeMillis()
        )
    }

    override suspend fun downloadLatestBackup(): DownloadResult = withContext(Dispatchers.IO) {
        val drive = createDriveClient() ?: return@withContext DownloadResult.Error("Kein Google-Konto verbunden.")

        val backupFile = runCatching {
            drive.files().list()
                .setSpaces(APP_DATA_SPACE)
                .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                .setOrderBy("modifiedTime desc")
                .setPageSize(1)
                .setFields("files(id)")
                .execute()
                .files
                ?.firstOrNull()
        }.getOrElse {
            return@withContext DownloadResult.Error("Backup-Datei konnte nicht aus Drive geladen werden.")
        } ?: return@withContext DownloadResult.NotFound

        val inputStream = runCatching {
            drive.files().get(backupFile.id).executeMediaAsInputStream()
        }.getOrElse {
            return@withContext DownloadResult.Error("Backup-Datei konnte nicht heruntergeladen werden.")
        }

        val content = inputStream.use { it.reader().readText() }
        DownloadResult.Success(content)
    }

    override fun isSignedIn(): Boolean = currentAccountEmail() != null

    override fun currentAccountEmail(): String? {
        val value = authPrefs.getString(KEY_ACCOUNT_EMAIL, null)?.trim().orEmpty()
        return value.ifBlank { null }
    }

    private fun createDriveClient(): Drive? {
        val accountEmail = currentAccountEmail() ?: return null
        credential.selectedAccountName = accountEmail
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Nozio")
            .build()
    }

    private fun resolveAuthorizationResult(
        authResult: AuthorizationResult,
        fallbackAccountEmail: String?
    ): DriveAuthState {
        if (authResult.hasResolution()) {
            val intentSender = authResult.pendingIntent?.intentSender
                ?: return DriveAuthState.Error("Google Drive Autorisierung konnte nicht gestartet werden.")
            return DriveAuthState.NeedsDriveAuthorization(intentSender)
        }

        val accountEmail = authResult.toGoogleSignInAccount()?.email ?: fallbackAccountEmail
        setSignedInAccountEmail(accountEmail)
        return DriveAuthState.Ready(accountEmail)
    }

    private fun mapApiExceptionToAuthError(e: ApiException): DriveAuthState.Error {
        return when (e.statusCode) {
            CommonStatusCodes.SIGN_IN_REQUIRED,
            CommonStatusCodes.INVALID_ACCOUNT -> DriveAuthState.Error(
                message = "Bitte erneut mit deinem Google-Konto anmelden.",
                reasonCode = DriveAuthReasonCode.SIGNED_OUT
            )

            CommonStatusCodes.CANCELED -> DriveAuthState.Error(
                message = "Google-Anmeldung wurde abgebrochen.",
                reasonCode = DriveAuthReasonCode.USER_CANCELED
            )

            CommonStatusCodes.NETWORK_ERROR,
            CommonStatusCodes.TIMEOUT,
            CommonStatusCodes.INTERRUPTED -> DriveAuthState.Error(
                message = "Netzwerkproblem bei der Google-Anmeldung.",
                reasonCode = DriveAuthReasonCode.NETWORK
            )

            CommonStatusCodes.DEVELOPER_ERROR -> DriveAuthState.Error(
                message = "Google OAuth ist nicht korrekt konfiguriert (Client-ID/SHA).",
                reasonCode = DriveAuthReasonCode.CONFIGURATION
            )

            CommonStatusCodes.ERROR -> DriveAuthState.Error(
                message = "Google Drive Berechtigung wurde nicht erteilt.",
                reasonCode = DriveAuthReasonCode.AUTHORIZATION_DENIED
            )

            else -> DriveAuthState.Error(
                message = "Google-Anmeldung fehlgeschlagen (Code ${e.statusCode}).",
                reasonCode = DriveAuthReasonCode.UNKNOWN
            )
        }
    }

    companion object {
        const val BACKUP_FILE_NAME: String = "nozio-backup-v1.json"
        private const val APP_DATA_SPACE = "appDataFolder"
        private const val AUTH_PREFS = "drive_backup_auth"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}
