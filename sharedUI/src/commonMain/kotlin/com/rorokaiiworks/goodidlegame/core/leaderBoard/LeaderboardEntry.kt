package com.rorokaiiworks.goodidlegame.core.leaderBoard

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val score: Long
)
