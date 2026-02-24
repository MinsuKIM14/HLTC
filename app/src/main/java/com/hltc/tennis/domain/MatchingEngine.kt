package com.hltc.tennis.domain

import com.hltc.tennis.data.MatchMode
import kotlin.math.abs
import kotlin.random.Random

data class CandidatePlayer(val id: Long, val level: Int)
data class GeneratedMatch(val teamA: List<Long>, val teamB: List<Long>)

class MatchingEngine(private val random: Random = Random.Default) {
    fun generate(players: List<CandidatePlayer>, totalGames: Int, mode: MatchMode): List<GeneratedMatch> {
        require(players.size >= 4)
        val appearances = players.associate { it.id to 0 }.toMutableMap()
        val partnerCount = mutableMapOf<Pair<Long, Long>, Int>()
        val result = mutableListOf<GeneratedMatch>()
        val recent = ArrayDeque<Long>()

        repeat(totalGames) {
            val selected = players.sortedWith(
                compareBy<CandidatePlayer> { appearances[it.id] ?: 0 }
                    .thenBy { recent.count { r -> r == it.id } }
                    .thenBy { random.nextInt() }
            ).take(4)
            val match = when (mode) {
                MatchMode.RANDOM -> {
                    val ids = selected.map { it.id }.shuffled(random)
                    GeneratedMatch(ids.take(2), ids.drop(2))
                }
                MatchMode.BALANCED -> bestBalanced(selected, partnerCount)
            }
            result += match
            (match.teamA + match.teamB).forEach {
                appearances[it] = (appearances[it] ?: 0) + 1
                recent.addLast(it)
            }
            while (recent.size > 12) recent.removeFirst()
            listOf(match.teamA, match.teamB).forEach { team ->
                val key = team.sorted().let { it[0] to it[1] }
                partnerCount[key] = (partnerCount[key] ?: 0) + 1
            }
        }
        return result
    }

    private fun bestBalanced(players: List<CandidatePlayer>, partnerCount: Map<Pair<Long, Long>, Int>): GeneratedMatch {
        val p = players
        val combos = listOf(
            GeneratedMatch(listOf(p[0].id, p[1].id), listOf(p[2].id, p[3].id)),
            GeneratedMatch(listOf(p[0].id, p[2].id), listOf(p[1].id, p[3].id)),
            GeneratedMatch(listOf(p[0].id, p[3].id), listOf(p[1].id, p[2].id))
        )
        val levelMap = players.associate { it.id to it.level }
        return combos.minWith(
            compareBy<GeneratedMatch> {
                abs(it.teamA.sumOf { id -> levelMap[id] ?: 0 } - it.teamB.sumOf { id -> levelMap[id] ?: 0 })
            }.thenBy {
                pairCount(it.teamA, partnerCount) + pairCount(it.teamB, partnerCount)
            }
        )
    }

    private fun pairCount(team: List<Long>, map: Map<Pair<Long, Long>, Int>): Int {
        val key = team.sorted().let { it[0] to it[1] }
        return map[key] ?: 0
    }
}
