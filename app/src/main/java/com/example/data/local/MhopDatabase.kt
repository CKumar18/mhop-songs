package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SongEntity::class, ImportHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MhopDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun importHistoryDao(): ImportHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MhopDatabase? = null

        fun getInstance(context: Context): MhopDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MhopDatabase::class.java,
                    "mhop_songs.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
