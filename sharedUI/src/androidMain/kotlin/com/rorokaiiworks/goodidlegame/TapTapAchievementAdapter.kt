package com.rorokaiiworks.goodidlegame

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.achievements.IAchievementAdapter
import com.taptap.sdk.achievement.TapAchievementCallback
import com.taptap.sdk.achievement.TapTapAchievement
import com.taptap.sdk.achievement.TapTapAchievementResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class TapTapAchievementAdapter : IAchievementAdapter, KoinComponent {
    private val logger: Logger by inject { parametersOf("TapTapAchievementAdapter") }

    init {
        val callback = object : TapAchievementCallback {
            override fun onAchievementSuccess(code: Int, result: TapTapAchievementResult?) {
                logger.i { "成就 ${result?.achievementId} 状态更新成功" }
            }

            override fun onAchievementFailure(achievementId: String, errorCode: Int, errorMessage: String) {
                logger.e { "成就 $achievementId 状态更新失败" }
            }
        }

        TapTapAchievement.registerCallback(callback = callback)
    }

    override fun setAchievement(achievementId: String) {
        TapTapAchievement.unlock(achievementId)
    }

    override fun clearAchievement(achievementId: String) {
        // No-op
    }
}