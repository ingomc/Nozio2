package de.ingomc.nozio

import android.app.Application
import de.ingomc.nozio.data.backup.BackupRepository
import de.ingomc.nozio.data.backup.BackupRepositoryImpl
import de.ingomc.nozio.data.backup.BackupScheduler
import de.ingomc.nozio.data.backup.AndroidBackupDocumentService
import de.ingomc.nozio.data.backup.BackupDocumentService
import de.ingomc.nozio.data.backup.DriveBackupService
import de.ingomc.nozio.data.backup.LocalFileBackupService
import de.ingomc.nozio.data.backup.RoomTrackingDataStore
import de.ingomc.nozio.data.local.NozioDatabase
import de.ingomc.nozio.data.remote.GitHubRetrofitInstance
import de.ingomc.nozio.data.remote.RetrofitInstance
import de.ingomc.nozio.data.repository.AppUpdateRepository
import de.ingomc.nozio.data.repository.DailyActivityRepository
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.FoodRepository
import de.ingomc.nozio.data.repository.MealTemplateRepository
import de.ingomc.nozio.data.repository.SupplementRepository
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.notifications.MealReminderReceiver
import de.ingomc.nozio.notifications.MealReminderScheduler
import de.ingomc.nozio.update.ApkUpdateInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

class NozioApplication : Application() {

    lateinit var database: NozioDatabase
        private set

    lateinit var foodRepository: FoodRepository
        private set

    lateinit var diaryRepository: DiaryRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var dailyActivityRepository: DailyActivityRepository
        private set

    lateinit var supplementRepository: SupplementRepository
        private set

    lateinit var mealTemplateRepository: MealTemplateRepository
        private set

    lateinit var appUpdateRepository: AppUpdateRepository
        private set

    lateinit var apkUpdateInstaller: ApkUpdateInstaller
        private set

    lateinit var driveBackupService: DriveBackupService
        private set

    lateinit var backupRepository: BackupRepository
        private set

    lateinit var backupDocumentService: BackupDocumentService
        private set

    lateinit var backupScheduler: BackupScheduler
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = NozioDatabase.getInstance(this)
        foodRepository = FoodRepository(RetrofitInstance.api, database.foodDao())
        diaryRepository = DiaryRepository(database.diaryDao())
        userPreferencesRepository = UserPreferencesRepository(this)
        dailyActivityRepository = DailyActivityRepository(database.dailyActivityDao())
        supplementRepository = SupplementRepository(database.supplementDao(), database.supplementIntakeDao())
        mealTemplateRepository = MealTemplateRepository(database.mealTemplateDao(), database.diaryDao(), database.foodDao())
        appUpdateRepository = AppUpdateRepository(GitHubRetrofitInstance.api)
        apkUpdateInstaller = ApkUpdateInstaller(this)
        driveBackupService = LocalFileBackupService(this)
        backupRepository = BackupRepositoryImpl(
            dataStore = RoomTrackingDataStore(database),
            appVersionName = BuildConfig.VERSION_NAME
        )
        backupDocumentService = AndroidBackupDocumentService(this)
        backupScheduler = BackupScheduler(this)
        MealReminderReceiver.ensureChannel(this)
        applicationScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            if (prefs.autoBackupEnabled) {
                backupScheduler.scheduleWeeklyBackup()
            } else {
                backupScheduler.cancelWeeklyBackup()
            }
            if (prefs.mealReminderEnabled) {
                MealReminderScheduler.scheduleDaily(this@NozioApplication, prefs.mealReminderHour, prefs.mealReminderMinute)
            } else {
                MealReminderScheduler.cancel(this@NozioApplication)
            }
        }
    }
}
