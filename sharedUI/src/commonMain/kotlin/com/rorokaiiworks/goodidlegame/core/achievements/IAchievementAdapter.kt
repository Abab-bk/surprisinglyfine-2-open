package com.rorokaiiworks.goodidlegame.core.achievements

interface IAchievementAdapter {
    fun setAchievement(achievementId: String)
    fun clearAchievement(achievementId: String)
}