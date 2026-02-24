package com.hltc.tennis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hltc.tennis.data.MatchEntity
import com.hltc.tennis.data.MatchMode
import com.hltc.tennis.data.PlayerEntity
import com.hltc.tennis.data.SessionEntity
import com.hltc.tennis.data.TennisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val players: List<PlayerEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val latestSessionMatches: List<MatchEntity> = emptyList(),
    val stats: Map<Long, com.hltc.tennis.data.PlayerStat> = emptyMap()
)

class MainViewModel(private val repo: TennisRepository) : ViewModel() {
    private val latestSession = repo.observeLatestSession()
    val uiState: StateFlow<MainUiState> = combine(
        repo.observePlayers(),
        repo.observeSessions(),
        latestSession,
        repo.observeTotalStats()
    ) { players, sessions, latest, stats ->
        MainUiState(players, sessions, emptyList(), stats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun seedDemo() = viewModelScope.launch {
        if (uiState.value.players.isNotEmpty()) return@launch
        listOf(
            "24기 민수" to 2, "24기 지수" to 3, "24기 도현" to 4, "24기 서연" to 2,
            "25기 하준" to 1, "25기 예린" to 5, "25기 현우" to 3, "25기 나은" to 4
        ).forEach {
            val parts = it.first.split(" ")
            repo.addPlayer(parts[0], parts[1], it.second)
        }
    }

    fun addPlayer(cohort: String, name: String, level: Int, note: String) = viewModelScope.launch {
        repo.addPlayer(cohort, name, level, note)
    }

    fun createSession(date: String, participantIds: List<Long>, totalGames: Int, mode: MatchMode) = viewModelScope.launch {
        repo.createSession(date, participantIds, totalGames, mode)
    }

    fun observeMatches(sessionId: Long): Flow<List<MatchEntity>> = repo.observeMatches(sessionId)

    fun saveResult(match: MatchEntity, a: Int, b: Int) = viewModelScope.launch { repo.saveResult(match, a, b) }
    fun resetResult(match: MatchEntity) = viewModelScope.launch { repo.resetResult(match) }
    fun updateLineup(match: MatchEntity, teamA: List<Long>, teamB: List<Long>) = viewModelScope.launch { repo.updateLineup(match, teamA, teamB) }
    fun moveMatch(match: MatchEntity, toIndex: Int) = viewModelScope.launch { repo.moveMatch(match, toIndex) }
}

class MainViewModelFactory(private val repo: TennisRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repo) as T
    }
}
