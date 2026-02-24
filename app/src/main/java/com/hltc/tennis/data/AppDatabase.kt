package com.hltc.tennis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlayerEntity::class, SessionEntity::class, MatchEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun sessionDao(): SessionDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hltc_tennis.db"
            ).build().also { INSTANCE = it }
        }
    }
}

fun List<Long>.toCsv(): String = joinToString(",")
fun String.toLongList(): List<Long> = if (isBlank()) emptyList() else split(",").mapNotNull { it.toLongOrNull() }
