package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity)

    @Query("DELETE FROM transfer_history")
    suspend fun clearAllHistory()

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)
}
