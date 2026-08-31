package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MinuteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MinuteDao {
    @Query("SELECT * FROM minutes ORDER BY createdAt DESC")
    fun getAllMinutes(): Flow<List<MinuteEntity>>

    @Query("SELECT * FROM minutes WHERE id = :id")
    suspend fun getMinuteById(id: Long): MinuteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMinute(minute: MinuteEntity): Long

    @Update
    suspend fun updateMinute(minute: MinuteEntity)

    @Query("DELETE FROM minutes WHERE id = :id")
    suspend fun deleteMinuteById(id: Long)

    @Query("SELECT COUNT(*) FROM minutes")
    suspend fun getMinutesCount(): Int
}
