package com.sheguard.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: Incident)

    @Query("SELECT * FROM incidents WHERE userId = :userId ORDER BY timestamp DESC")
    fun getIncidents(userId: String): Flow<List<Incident>>
}
