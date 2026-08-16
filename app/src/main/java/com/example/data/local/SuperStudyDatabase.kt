package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        TestSessionEntity::class,
        SavedAttemptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SuperStudyDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun testSessionDao(): TestSessionDao
    abstract fun savedAttemptDao(): SavedAttemptDao

    companion object {
        @Volatile
        private var INSTANCE: SuperStudyDatabase? = null

        fun getInstance(context: Context): SuperStudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuperStudyDatabase::class.java,
                    "super_study.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
