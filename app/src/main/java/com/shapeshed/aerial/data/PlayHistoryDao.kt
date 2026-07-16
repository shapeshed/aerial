package com.shapeshed.aerial.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlay(entry: PlayHistoryEntry)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<PlayHistoryEntry>

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun recentAsFlow(limit: Int): Flow<List<PlayHistoryEntry>>
}
