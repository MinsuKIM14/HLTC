package com.hltc.tennis.domain

import com.hltc.tennis.data.MatchMode
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingEngineTest {
    @Test
    fun `균등 출전 우선 유지`() {
        val players = (1L..8L).map { CandidatePlayer(it, 3) }
        val matches = MatchingEngine(Random(0)).generate(players, totalGames = 8, mode = MatchMode.RANDOM)
        val counts = mutableMapOf<Long, Int>()
        matches.flatMap { it.teamA + it.teamB }.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        assertTrue((counts.values.max() - counts.values.min()) <= 1)
    }

    @Test
    fun `밸런스 모드에서 팀 레벨 차 최소화`() {
        val players = listOf(
            CandidatePlayer(1, 5), CandidatePlayer(2, 1), CandidatePlayer(3, 4), CandidatePlayer(4, 2)
        )
        val match = MatchingEngine(Random(1)).generate(players, 1, MatchMode.BALANCED).first()
        val level = players.associate { it.id to it.level }
        val diff = kotlin.math.abs(match.teamA.sumOf { level[it]!! } - match.teamB.sumOf { level[it]!! })
        assertTrue(diff <= 0)
    }

    @Test
    fun `랜덤 모드에서도 4명 중복 없음`() {
        val players = (1L..6L).map { CandidatePlayer(it, 3) }
        val matches = MatchingEngine(Random(2)).generate(players, 4, MatchMode.RANDOM)
        matches.forEach { m ->
            assertTrue((m.teamA + m.teamB).toSet().size == 4)
        }
    }
}
