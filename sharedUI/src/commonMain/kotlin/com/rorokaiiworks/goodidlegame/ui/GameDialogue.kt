package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle

@Composable
fun TextDialogue(
    title: String,
    text: String,
    onConfirm: () -> Unit = {}
) {
    BaseCard(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CardTitle(title)

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConfirm,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("Confirm")
        }
    }
}


@Composable
@Preview
private fun TextDialoguePreview() {
    IdleGameTheme {
        TextDialogue(
            title = "Test Title",
            text = "Test Label"
        )
    }
}