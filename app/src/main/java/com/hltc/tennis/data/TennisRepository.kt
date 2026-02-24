package com.hltc.tennis.data

import com.hltc.tennis.domain.CandidatePlayer
import com.hltc.tennis.domain.MatchingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class PlayerStat(val playerId: Long, val matches: Int, val wins: Int, val losses: Int) {
    val winRate: Float = if (matches == 0) 0f else wins.toFloat() / matches
}

class TennisRepository(
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao,
    private val matchDao: MatchDao,
    private val engine: MatchingEngine = MatchingEngine()
) {
    fun observePlayers() = playerDao.observePlayers()
    fun observeSessions() = sessionDao.observeSessions()
    fun observeLatestSession() = sessionDao.observeLatestSession()
    fun observeMatches(sessionId: Long) = matchDao.observeMatchesBySession(sessionId)

    fun observeTotalStats(): Flow<Map<Long, PlayerStat>> = matchDao.observeCompletedMatches().map { matches ->
        buildStats(matches)
    }

    suspend fun addPlayer(cohort: String, name: String, level: Int, note: String = "") {
        playerDao.insert(PlayerEntity(cohort = cohort, name = name, level = level.coerceIn(1, 5), note = note))
    }

    suspend fun updatePlayer(player: PlayerEntity) = playerDao.update(player.copy(updatedAt = System.currentTimeMillis()))

    suspend fun createSession(date: String, participantIds: List<Long>, totalGames: Int, mode: MatchMode): Long {
        val sessionId = sessionDao.insert(
            SessionEntity(date = date, mode = mode.name, totalGames = totalGames, participantIds = participantIds.toCsv())
        )
        val players = playerDao.findByIds(participantIds).map { CandidatePlayer(it.id, it.level) }
        val generated = engine.generate(players, totalGames, mode)
        matchDao.insertAll(generated.mapIndexed { idx, gm ->
            MatchEntity(
                sessionId = sessionId,
                orderIndex = idx,
                teamAPlayerIds = gm.teamA.toCsv(),
                teamBPlayerIds = gm.teamB.toCsv()
            )
        })
        return sessionId
    }

    suspend fun saveResult(match: MatchEntity, scoreA: Int, scoreB: Int) {
        val winner = if (scoreA >= scoreB) "A" else "B"
        matchDao.update(match.copy(status = MatchStatus.COMPLETED.name, scoreA = scoreA, scoreB = scoreB, winner = winner))
    }

    suspend fun resetResult(match: MatchEntity) {
        matchDao.update(match.copy(status = MatchStatus.PLANNED.name, scoreA = null, scoreB = null, winner = null))
    }

    suspend fun moveMatch(match: MatchEntity, toIndex: Int) {
        matchDao.updateOrder(match.id, toIndex)
    }

    suspend fun updateLineup(match: MatchEntity, teamA: List<Long>, teamB: List<Long>): Boolean {
        val all = teamA + teamB
        if (all.size != 4 || all.toSet().size != 4 || match.status == MatchStatus.COMPLETED.name) return false
        matchDao.update(match.copy(teamAPlayerIds = teamA.toCsv(), teamBPlayerIds = teamB.toCsv()))
        return true
    }

    suspend fun rebalancePlanned(session: SessionEntity) {
        val participants = playerDao.findByIds(session.participantIds.toLongList())
        val plannedPlayers = participants.map { CandidatePlayer(it.id, it.level) }
        matchDao.deletePlannedBySession(session.id)
        val generated = engine.generate(plannedPlayers, session.totalGames, MatchMode.valueOf(session.mode))
        matchDao.insertAll(generated.mapIndexed { idx, gm ->
            MatchEntity(sessionId = session.id, orderIndex = idx, teamAPlayerIds = gm.teamA.toCsv(), teamBPlayerIds = gm.teamB.toCsv())
        })
    }

    private fun buildStats(matches: List<MatchEntity>): Map<Long, PlayerStat> {
        val mutable = mutableMapOf<Long, PlayerStat>()
        matches.filter { it.status == MatchStatus.COMPLETED.name && it.winner != null }.forEach { m ->
            val winners = if (m.winner == "A") m.teamAPlayerIds.toLongList() else m.teamBPlayerIds.toLongList()
            val losers = if (m.winner == "A") m.teamBPlayerIds.toLongList() else m.teamAPlayerIds.toLongList()
            (winners + losers).forEach { id ->
                val prev = mutable[id] ?: PlayerStat(id, 0, 0, 0)
                mutable[id] = prev.copy(matches = prev.matches + 1)
            }
            winners.forEach { id ->
                val prev = mutable[id]!!
                mutable[id] = prev.copy(wins = prev.wins + 1)
            }
            losers.forEach { id ->
                val prev = mutable[id]!!
                mutable[id] = prev.copy(losses = prev.losses + 1)
            }
        }
        return mutable
    }
}
