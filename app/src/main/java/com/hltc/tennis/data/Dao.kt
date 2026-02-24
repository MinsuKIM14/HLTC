package com.hltc.tennis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY active DESC, cohort ASC, name ASC")
    fun observePlayers(): Flow<List<PlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun update(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<PlayerEntity>
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestSession(): Flow<SessionEntity?>
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun observeMatchesBySession(sessionId: Long): Flow<List<MatchEntity>>

    @Insert
    suspend fun insertAll(matches: List<MatchEntity>)

    @Update
    suspend fun update(match: MatchEntity)

    @Query("UPDATE matches SET orderIndex = :toIndex WHERE id = :matchId")
    suspend fun updateOrder(matchId: Long, toIndex: Int)

    @Query("SELECT * FROM matches WHERE status = 'COMPLETED'")
    fun observeCompletedMatches(): Flow<List<MatchEntity>>

    @Query("DELETE FROM matches WHERE sessionId = :sessionId AND status = 'PLANNED'")
    suspend fun deletePlannedBySession(sessionId: Long)
}
