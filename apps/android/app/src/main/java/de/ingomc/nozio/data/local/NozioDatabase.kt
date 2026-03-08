package de.ingomc.nozio.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodItem::class, DiaryEntry::class, DailyActivity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NozioDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun dailyActivityDao(): DailyActivityDao

    suspend fun replaceTrackingData(
        foodItems: List<FoodItem>,
        diaryEntries: List<DiaryEntry>,
        dailyActivities: List<DailyActivity>
    ) {
        withTransaction {
            diaryDao().deleteAll()
            dailyActivityDao().deleteAll()
            foodDao().deleteAll()

            foodDao().insertAllWithIds(foodItems)
            diaryDao().insertAll(diaryEntries)
            dailyActivityDao().upsertAll(dailyActivities)
        }
    }

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_activity (
                        date TEXT NOT NULL,
                        steps INTEGER NOT NULL,
                        PRIMARY KEY(date)
                    )
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE daily_activity
                    ADD COLUMN weightKg REAL
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN servingSize TEXT")
                db.execSQL("ALTER TABLE food_items ADD COLUMN servingQuantity REAL")
                db.execSQL("ALTER TABLE food_items ADD COLUMN packageSize TEXT")
                db.execSQL("ALTER TABLE food_items ADD COLUMN packageQuantity REAL")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN imageUrl TEXT")
            }
        }

        @Volatile
        private var INSTANCE: NozioDatabase? = null

        fun getInstance(context: Context): NozioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NozioDatabase::class.java,
                    "nozio_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
