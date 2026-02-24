package com.hltc.tennis

import android.content.Context
import com.hltc.tennis.data.AppDatabase
import com.hltc.tennis.data.TennisRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)
    val repository = TennisRepository(db.playerDao(), db.sessionDao(), db.matchDao())
}
