package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CheatEntity
import com.example.data.model.DownloadTaskEntity
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity

@Database(
    entities = [
        GameEntity::class,
        SaveStateEntity::class,
        CheatEntity::class,
        DownloadTaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AtariDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun saveStateDao(): SaveStateDao
    abstract fun cheatDao(): CheatDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AtariDatabase? = null

        fun getInstance(context: Context): AtariDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtariDatabase::class.java,
                    "atari_classics.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
