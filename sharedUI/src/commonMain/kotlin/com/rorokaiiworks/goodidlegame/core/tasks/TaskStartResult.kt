package com.rorokaiiworks.goodidlegame.core.tasks

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.rorokaiiworks.goodidlegame.core.tasks.TaskStartResult.*
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper

enum class TaskStartResult(
    val buttonText: String,
    val enabled: Boolean
) {
    Success(i18nWrapper("Start"), true),
    InventoryFull(i18nWrapper("Inventory full"), false),
    SkillLevelNotMet(i18nWrapper("Skill level too low"), false),
    ConsumeItemsNotMet(i18nWrapper("Not enough items"), false),
}

@Composable
fun TaskStartResult.getResultButtonColors(): ButtonColors {
    return when (this) {
        Success -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        InventoryFull,
        SkillLevelNotMet,
        ConsumeItemsNotMet -> {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}