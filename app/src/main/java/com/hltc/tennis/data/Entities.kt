package com.hltc.tennis.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cohort: String,
    val name: String,
    val level: Int,
    val note: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val mode: String,
    val totalGames: Int,
    val participantIds: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "matches",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val status: String = MatchStatus.PLANNED.name,
    val teamAPlayerIds: String,
    val teamBPlayerIds: String,
    val scoreA: Int? = null,
    val scoreB: Int? = null,
    val winner: String? = null
)

enum class MatchStatus { PLANNED, COMPLETED }
enum class MatchMode { RANDOM, BALANCED }
