package de.ingomc.nozio.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodItem::class, DiaryEntry::class, DailyActivity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NozioDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun dailyActivityDao(): DailyActivityDao

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

        @Volatile
        private var INSTANCE: NozioDatabase? = null

        fun getInstance(context: Context): NozioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NozioDatabase::class.java,
                    "nozio_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
