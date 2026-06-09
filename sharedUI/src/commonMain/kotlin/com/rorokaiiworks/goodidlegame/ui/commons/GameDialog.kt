package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun GameDialog(
    title: String,
    isDangerous: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirmation: (() -> Unit)? = null,
    i18n: I18n = koinInject(),
    fullScreen: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = if (fullScreen) DialogProperties(usePlatformDefaultWidth = false)
        else DialogProperties(),
    ) {
        BaseCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardTitle(title)

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                content()
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onConfirmation != null) {
                    FilledTonalButton(
                        enabled = confirmEnabled,
                        colors = if (isDangerous) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier.weight(1f),
                        onClick = onConfirmation,
                    ) {
                        Text(
                            text = i18n.tr("Confirm")
                        )
                    }
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest
                ) {
                    Text(i18n.tr("Cancel"))
                }
            }
        }
    }
}
