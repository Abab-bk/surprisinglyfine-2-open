package com.rorokaiiworks.goodidlegame

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.codedisaster.steamworks.SteamResult
import com.codedisaster.steamworks.SteamUserStats
import com.codedisaster.steamworks.SteamUserStatsCallback
import com.rorokaiiworks.goodidlegame.core.achievements.IAchievementAdapter

class SteamAchievementsAdapter(
    private val userStats: SteamUserStats,
    steamStatsManager: SteamStatsManager
) : IAchievementAdapter {
    private val logger = Logger(
        config = loggerConfigInit(platformLogWriter(NoTagFormatter)),
        tag = "SteamAchievementsAdapter"
    )

    private val callbackSubscription = steamStatsManager.register(object : SteamUserStatsCallback {
        override fun onUserStatsStored(gameId: Long, result: SteamResult?) {
            logger.i { "onUserStatsStored: $gameId, $result" }
        }

        override fun onUserAchievementStored(
            gameId: Long,
            isGroupAchievement: Boolean,
            achievementName: String?,
            curProgress: Int,
            maxProgress: Int
        ) {
            logger.i { "onUserAchievementStored: $gameId, $isGroupAchievement, $achievementName, $curProgress, $maxProgress" }
        }
    })

    override fun setAchievement(achievementId: String) {
        userStats.setAchievement(achievementId)
        userStats.storeStats()
    }

    override fun clearAchievement(achievementId: String) {
        userStats.clearAchievement(achievementId)
        userStats.storeStats()
    }
}
