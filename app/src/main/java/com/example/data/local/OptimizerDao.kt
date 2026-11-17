package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OptimizerDao {
    @Query("SELECT * FROM optimization_history ORDER BY timestampMs DESC LIMIT 50")
    fun observeHistory(): Flow<List<OptimizationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: OptimizationHistoryEntity): Long

    @Query("DELETE FROM optimization_history")
    suspend fun clearHistory()

    @Query("DELETE FROM optimization_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("SELECT * FROM optimization_profiles ORDER BY isBuiltIn DESC, id ASC")
    fun observeProfiles(): Flow<List<OptimizationProfileEntity>>

    @Query("SELECT COUNT(*) FROM optimization_profiles")
    suspend fun getProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: OptimizationProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<OptimizationProfileEntity>)

    @Update
    suspend fun updateProfile(profile: OptimizationProfileEntity)

    @Query("DELETE FROM optimization_profiles WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomProfile(id: Long)
}
