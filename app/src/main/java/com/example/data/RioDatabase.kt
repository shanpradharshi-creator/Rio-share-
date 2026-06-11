package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TransferEntity::class], version = 1, exportSchema = false)
abstract class RioDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: RioDatabase? = null

        fun getDatabase(context: Context): RioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RioDatabase::class.java,
                    "rio_share_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
