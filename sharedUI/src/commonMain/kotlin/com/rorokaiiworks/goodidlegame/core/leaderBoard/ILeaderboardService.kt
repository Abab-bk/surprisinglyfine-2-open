package com.rorokaiiworks.goodidlegame.core.leaderBoard

import com.rorokaiiworks.goodidlegame.core.Resource

interface ILeaderboardService {
    suspend fun uploadScore(leaderboardName: String, score: Int): Resource<Boolean>
    suspend fun fetchTopScores(leaderboardName: String, count: Int): Resource<List<LeaderboardEntry>>
    suspend fun getPlayerRank(leaderboardName: String): Resource<LeaderboardEntry?>
}
