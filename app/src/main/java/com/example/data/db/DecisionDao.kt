package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DecisionEntity
import com.example.data.model.DecisionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions ORDER BY dueDateMillis ASC")
    fun getAllDecisions(): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE minuteId = :minuteId ORDER BY id ASC")
    fun getDecisionsForMinute(minuteId: Long): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE status != 'COMPLETED' ORDER BY dueDateMillis ASC")
    fun getPendingDecisions(): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE status != 'COMPLETED' AND dueDateMillis < :currentTimeMillis ORDER BY dueDateMillis ASC")
    fun getOverdueDecisions(currentTimeMillis: Long): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE id = :id")
    suspend fun getDecisionById(id: Long): DecisionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecisions(decisions: List<DecisionEntity>): List<Long>

    @Update
    suspend fun updateDecision(decision: DecisionEntity)

    @Query("UPDATE decisions SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DecisionStatus, completedAt: Long?)

    @Query("DELETE FROM decisions WHERE id = :id")
    suspend fun deleteDecisionById(id: Long)

    @Query("DELETE FROM decisions WHERE minuteId = :minuteId")
    suspend fun deleteDecisionsForMinute(minuteId: Long)
}
