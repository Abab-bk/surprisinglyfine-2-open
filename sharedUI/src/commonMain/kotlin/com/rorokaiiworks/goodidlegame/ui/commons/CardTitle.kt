package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.X

@Composable
fun CardTitle(
    title: String,
    content: @Composable () -> Unit = { },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .padding(
                    bottom = 8.dp
                )
                .weight(1f),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        content()
    }
}

@Composable
fun CardTitleWithCloseBtn(
    title: String,
    onClose: () -> Unit,
) {
    CardTitle(
        title = title,
        content = {
            IconButton(
                modifier = Modifier.heightIn(max = 28.dp),
                onClick = onClose
            ) {
                Icon(
                    imageVector = Feather.X,
                    contentDescription = "Close skill description"
                )
            }
        }
    )
}

@Composable
fun CardTitleWithButton(
    title: String,
    contentDescription: String,
    imageVector: ImageVector,
    onButtonClick: () -> Unit,
) {
    CardTitle(
        title = title,
        content = {
            IconButton(
                modifier = Modifier.heightIn(max = 28.dp),
                onClick = onButtonClick
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription
                )
            }
        }
    )
}
