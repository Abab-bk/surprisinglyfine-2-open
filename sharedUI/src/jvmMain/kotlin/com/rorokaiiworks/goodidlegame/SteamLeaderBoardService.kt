package com.rorokaiiworks.goodidlegame

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.codedisaster.steamworks.*
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.leaderBoard.ILeaderboardService
import com.rorokaiiworks.goodidlegame.core.leaderBoard.LeaderboardEntry
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class SteamLeaderboardService(
    private val userStats: SteamUserStats,
    private val steamUser: SteamUser,
    private val steamFriends: SteamFriends,
    steamStatsManager: SteamStatsManager
) : ILeaderboardService {
    private val logger = Logger(
        config = loggerConfigInit(platformLogWriter(NoTagFormatter)),
        tag = "SteamLeaderboardService"
    )

    private val leaderboardHandles = mutableMapOf<String, SteamLeaderboardHandle>()

    private val pendingFindRequests = mutableMapOf<String, MutableList<Continuation<SteamLeaderboardHandle?>>>()
    private val pendingUploadRequests = mutableMapOf<SteamLeaderboardHandle, Continuation<Resource<Boolean>>>()
    private val pendingDownloadRequests = mutableMapOf<SteamLeaderboardHandle, Continuation<DownloadResult>>()

    private val callbackSubscription = steamStatsManager.register(object : SteamUserStatsCallback {
        override fun onLeaderboardFindResult(leaderboard: SteamLeaderboardHandle, found: Boolean) {
            logger.i { "Leaderboard find result: $found" }
            handleLeaderboardFound(leaderboard, found)
        }

        override fun onLeaderboardScoresDownloaded(
            leaderboard: SteamLeaderboardHandle,
            entries: SteamLeaderboardEntriesHandle,
            numEntries: Int
        ) {
            logger.i { "Scores downloaded: $numEntries entries" }
            handleScoresDownloaded(leaderboard, entries, numEntries)
        }

        override fun onLeaderboardScoreUploaded(
            success: Boolean,
            leaderboard: SteamLeaderboardHandle,
            score: Int,
            scoreChanged: Boolean,
            globalRankNew: Int,
            globalRankPrevious: Int
        ) {
            logger.i { "Score uploaded: success=$success, rank=$globalRankNew" }
            handleScoreUploaded(leaderboard, success, globalRankNew)
        }
    })

    override suspend fun uploadScore(
        leaderboardName: String,
        score: Int
    ): Resource<Boolean> = try {
        val handle = findLeaderboard(leaderboardName)
            ?: return Resource.Error(404, "Leaderboard not found")

        suspendCancellableCoroutine { continuation ->
            pendingUploadRequests[handle] = continuation

            continuation.invokeOnCancellation {
                pendingUploadRequests.remove(handle)
            }

            userStats.uploadLeaderboardScore(
                handle,
                SteamUserStats.LeaderboardUploadScoreMethod.KeepBest,
                score,
                intArrayOf()
            )
        }
    } catch (e: Exception) {
        logger.e(e) { "Error uploading score" }
        Resource.Error(500, e.message ?: "Unknown error")
    }

    override suspend fun fetchTopScores(
        leaderboardName: String,
        count: Int
    ): Resource<List<LeaderboardEntry>> = try {
        logger.i { "Fetching friend scores for $leaderboardName" }

        val handle = findLeaderboard(leaderboardName)
            ?: return Resource.Error(404, "Leaderboard not found")

        val totalEntries = runCatching { userStats.getLeaderboardEntryCount(handle) }
            .getOrDefault(-1)
        logger.i { "Leaderboard total entries: $totalEntries" }

        val result = downloadEntries(handle) {
            userStats.downloadLeaderboardEntries(
                handle,
                SteamUserStats.LeaderboardDataRequest.Friends,
                0,
                (count - 1).coerceAtLeast(0)
            )
        }

        val friendRanked = result.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }

        Resource.Success(friendRanked)
    } catch (e: Exception) {
        logger.e(e) { "Error fetching scores" }
        Resource.Error(500, e.message ?: "Unknown error")
    }

    override suspend fun getPlayerRank(
        leaderboardName: String,
    ): Resource<LeaderboardEntry?> = try {
        val handle = findLeaderboard(leaderboardName)
            ?: return Resource.Error(404, "Leaderboard not found")

        val entries = downloadEntries(handle) {
            userStats.downloadLeaderboardEntriesForUsers(handle, arrayOf(steamUser.steamID))
        }

        Resource.Success(entries.firstOrNull())
    } catch (e: Exception) {
        logger.e(e) { "Error getting player rank" }
        Resource.Error(500, e.message ?: "Unknown error")
    }

    private suspend fun findLeaderboard(name: String): SteamLeaderboardHandle? {
        // 返回缓存的句柄
        leaderboardHandles[name]?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            // 检查是否已有相同名称的请求在处理中
            val pending = pendingFindRequests.getOrPut(name) { mutableListOf() }
            pending.add(continuation)

            continuation.invokeOnCancellation {
                pending.remove(continuation)
                if (pending.isEmpty()) {
                    pendingFindRequests.remove(name)
                }
            }

            // 如果是第一个请求，发起查找
            if (pending.size == 1) {
                userStats.findLeaderboard(name)
            }
        }
    }

    private suspend fun downloadEntries(
        handle: SteamLeaderboardHandle,
        downloadAction: () -> Unit
    ): List<LeaderboardEntry> {
        val result = suspendCancellableCoroutine { continuation ->
            pendingDownloadRequests[handle] = continuation

            continuation.invokeOnCancellation {
                pendingDownloadRequests.remove(handle)
            }

            downloadAction()
        }

        return result.entries
    }

    private fun handleLeaderboardFound(handle: SteamLeaderboardHandle, found: Boolean) {
        // 查找对应的排行榜名称
        val name = pendingFindRequests.keys.firstOrNull { key ->
            pendingFindRequests[key]?.isNotEmpty() == true
        } ?: return

        val pending = pendingFindRequests.remove(name) ?: return

        if (found) {
            leaderboardHandles[name] = handle
        }

        // 通知所有等待的请求
        pending.forEach { continuation ->
            continuation.resume(if (found) handle else null)
        }
    }

    private fun handleScoresDownloaded(
        leaderboard: SteamLeaderboardHandle,
        entriesHandle: SteamLeaderboardEntriesHandle?,
        numEntries: Int
    ) {
        val continuation = pendingDownloadRequests.remove(leaderboard) ?: return

        if (entriesHandle == null || numEntries == 0) {
            continuation.resume(DownloadResult(emptyList()))
            return
        }

        try {
            val entries = (0 until numEntries).map { i ->
                val entry = SteamLeaderboardEntry()
                userStats.getDownloadedLeaderboardEntry(entriesHandle, i, entry, intArrayOf())

                LeaderboardEntry(
                    rank = entry.globalRank,
                    score = entry.score.toLong(),
                    userId = entry.steamIDUser.accountID.toString(),
                    userName = steamFriends.getFriendPersonaName(entry.steamIDUser)
                )
            }

            logger.i { "Fetched ${entries.size} leaderboard entries" }
            continuation.resume(DownloadResult(entries))
        } catch (e: Exception) {
            logger.e(e) { "Error parsing leaderboard entries" }
            continuation.resume(DownloadResult(emptyList(), e))
        }
    }

    private fun handleScoreUploaded(
        leaderboard: SteamLeaderboardHandle,
        success: Boolean,
        rank: Int
    ) {
        val continuation = pendingUploadRequests.remove(leaderboard) ?: return

        if (success) {
            logger.i { "Score uploaded successfully. New rank: $rank" }
            continuation.resume(Resource.Success(true))
        } else {
            logger.e { "Failed to upload score" }
            continuation.resume(Resource.Error(500, "Failed to upload score"))
        }
    }

    private data class DownloadResult(
        val entries: List<LeaderboardEntry>,
        val error: Exception? = null
    )
}
