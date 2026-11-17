package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OptimizationHistoryEntity::class,
        OptimizationProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OptimizerDatabase : RoomDatabase() {
    abstract fun optimizerDao(): OptimizerDao

    companion object {
        @Volatile
        private var INSTANCE: OptimizerDatabase? = null

        fun getInstance(context: Context): OptimizerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OptimizerDatabase::class.java,
                    "ff_pro_optimizer.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
