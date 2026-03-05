package de.ingomc.nozio.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
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
    private val driveScope = Scope(DriveScopes.DRIVE_APPDATA)
    private val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
        appContext,
        listOf(DriveScopes.DRIVE_APPDATA)
    )

    private val signInClient by lazy {
        GoogleSignIn.getClient(
            appContext,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(driveScope)
                .build()
        )
    }

    override suspend fun ensureSignedIn(): SignInResult = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(appContext)
        if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
            credential.selectedAccount = account.account
            return@withContext SignInResult.SignedIn(account.email)
        }
        SignInResult.RequiresUserAction(signInClient.signInIntent)
    }

    override suspend fun completeSignIn(signInResultData: Intent?): SignInResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(signInResultData).result
            if (account == null || !GoogleSignIn.hasPermissions(account, driveScope)) {
                SignInResult.Error("Google Drive Berechtigung wurde nicht erteilt.")
            } else {
                credential.selectedAccount = account.account
                SignInResult.SignedIn(account.email)
            }
        } catch (_: ApiException) {
            SignInResult.Error("Google-Anmeldung fehlgeschlagen.")
        }
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

    override fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(appContext) ?: return false
        return GoogleSignIn.hasPermissions(account, driveScope)
    }

    override fun currentAccountEmail(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(appContext)
        return account?.email
    }

    private fun createDriveClient(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(appContext) ?: return null
        if (!GoogleSignIn.hasPermissions(account, driveScope)) return null
        credential.selectedAccount = account.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Nozio")
            .build()
    }

    companion object {
        const val BACKUP_FILE_NAME: String = "nozio-backup-v1.json"
        private const val APP_DATA_SPACE = "appDataFolder"
    }
}
