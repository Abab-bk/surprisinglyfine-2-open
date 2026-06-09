package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.runtime.mutableStateMapOf
import com.rorokaiiworks.goodidlegame.ui.quests.QuestCategory

sealed class NotificationType(val title: String, val good: Boolean) {
    class Default : NotificationType("", true)

    class QuestCompleted(category: QuestCategory) : NotificationType(i18nWrapper("Completed"), true)
    object JourneyCompleted : NotificationType(i18nWrapper("Completed"), true)

    object CityBankrupted : NotificationType(i18nWrapper("Bankrupted"), false)
}

class Notifier {
    val badgeCountMap = mutableStateMapOf<String, NotificationType>()

    fun updateBadge(route: String, type: NotificationType?) {
        if (type == null) {
            badgeCountMap.remove(route)
            return
        }

        badgeCountMap[route] = type
    }
}