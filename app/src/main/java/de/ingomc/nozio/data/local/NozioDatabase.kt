package de.ingomc.nozio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [FoodItem::class, DiaryEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NozioDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: NozioDatabase? = null

        fun getInstance(context: Context): NozioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NozioDatabase::class.java,
                    "nozio_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

