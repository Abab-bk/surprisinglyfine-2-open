package com.rorokaiiworks.goodidlegame.core.leaderBoard

import com.rorokaiiworks.goodidlegame.core.Resource

class FakeLeaderBoardService : ILeaderboardService {
    override suspend fun uploadScore(
        leaderboardName: String,
        score: Int
    ): Resource<Boolean> {
        return Resource.Success(true)
    }

    override suspend fun fetchTopScores(
        leaderboardName: String,
        count: Int
    ): Resource<List<LeaderboardEntry>> {
        return Resource.Success(emptyList())
    }

    override suspend fun getPlayerRank(leaderboardName: String): Resource<LeaderboardEntry?> {
        return Resource.Success(null)
    }
}