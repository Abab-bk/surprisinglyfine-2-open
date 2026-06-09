package com.rorokaiiworks.goodidlegame

import com.codedisaster.steamworks.SteamID
import com.codedisaster.steamworks.SteamLeaderboardEntriesHandle
import com.codedisaster.steamworks.SteamLeaderboardHandle
import com.codedisaster.steamworks.SteamResult
import com.codedisaster.steamworks.SteamUserStatsCallback
import java.util.concurrent.CopyOnWriteArraySet

class SteamStatsManager : SteamUserStatsCallback {
    private val listeners = CopyOnWriteArraySet<SteamUserStatsCallback>()

    fun register(listener: SteamUserStatsCallback): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }

    fun unregister(listener: SteamUserStatsCallback) {
        listeners.remove(listener)
    }

    override fun onUserStatsReceived(gameId: Long, steamIDUser: SteamID?, result: SteamResult?) {
        listeners.forEach { it.onUserStatsReceived(gameId, steamIDUser, result) }
    }

    override fun onUserStatsStored(gameId: Long, result: SteamResult?) {
        listeners.forEach { it.onUserStatsStored(gameId, result) }
    }

    override fun onUserStatsUnloaded(steamIDUser: SteamID?) {
        listeners.forEach { it.onUserStatsUnloaded(steamIDUser) }
    }

    override fun onUserAchievementStored(
        gameId: Long,
        isGroupAchievement: Boolean,
        achievementName: String?,
        curProgress: Int,
        maxProgress: Int
    ) {
        listeners.forEach {
            it.onUserAchievementStored(
                gameId,
                isGroupAchievement,
                achievementName,
                curProgress,
                maxProgress
            )
        }
    }

    override fun onLeaderboardFindResult(leaderboard: SteamLeaderboardHandle?, found: Boolean) {
        listeners.forEach { it.onLeaderboardFindResult(leaderboard, found) }
    }

    override fun onLeaderboardScoresDownloaded(
        leaderboard: SteamLeaderboardHandle?,
        entries: SteamLeaderboardEntriesHandle?,
        numEntries: Int
    ) {
        listeners.forEach { it.onLeaderboardScoresDownloaded(leaderboard, entries, numEntries) }
    }

    override fun onLeaderboardScoreUploaded(
        success: Boolean,
        leaderboard: SteamLeaderboardHandle?,
        score: Int,
        scoreChanged: Boolean,
        globalRankNew: Int,
        globalRankPrevious: Int
    ) {
        listeners.forEach {
            it.onLeaderboardScoreUploaded(
                success,
                leaderboard,
                score,
                scoreChanged,
                globalRankNew,
                globalRankPrevious
            )
        }
    }

    override fun onNumberOfCurrentPlayersReceived(success: Boolean, players: Int) {
        listeners.forEach { it.onNumberOfCurrentPlayersReceived(success, players) }
    }

    override fun onGlobalStatsReceived(gameId: Long, result: SteamResult?) {
        listeners.forEach { it.onGlobalStatsReceived(gameId, result) }
    }

}
