package de.ingomc.nozio

import android.app.Application
import de.ingomc.nozio.data.local.NozioDatabase
import de.ingomc.nozio.data.remote.RetrofitInstance
import de.ingomc.nozio.data.repository.DailyActivityRepository
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.FoodRepository
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.notifications.MealReminderReceiver
import de.ingomc.nozio.notifications.MealReminderScheduler
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = NozioDatabase.getInstance(this)
        foodRepository = FoodRepository(RetrofitInstance.api, database.foodDao())
        diaryRepository = DiaryRepository(database.diaryDao())
        userPreferencesRepository = UserPreferencesRepository(this)
        dailyActivityRepository = DailyActivityRepository(database.dailyActivityDao())
        MealReminderReceiver.ensureChannel(this)
        applicationScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            if (prefs.mealReminderEnabled) {
                MealReminderScheduler.scheduleDaily(this@NozioApplication, prefs.mealReminderHour, prefs.mealReminderMinute)
            } else {
                MealReminderScheduler.cancel(this@NozioApplication)
            }
        }
    }
}
