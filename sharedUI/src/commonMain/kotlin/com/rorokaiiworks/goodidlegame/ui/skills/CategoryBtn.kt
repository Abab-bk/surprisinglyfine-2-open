package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import goodidlegame.sharedui.generated.resources.Res
import goodidlegame.sharedui.generated.resources.allStringResources
import org.jetbrains.compose.resources.stringResource

@Composable
fun CategoryBtn(
    modifier: Modifier = Modifier,
    category: String?,
    isSelected: Boolean,
    badge: @Composable BoxScope.() -> Unit = { },
    onClick: () -> Unit,
) {
    if (category == null) return

    Surface(
        color =
            if (isSelected) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
    ) {
        BadgedBox(
            badge = badge,
        ) {
            Res.allStringResources[category]?.let { Text(stringResource(it)) } ?:
            Text(category.replaceFirstChar { it.uppercase() })
        }
    }
}
