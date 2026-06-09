package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.ui.PreviewConstants
import com.rorokaiiworks.goodidlegame.ui.commons.DefaultHorizontalDivider
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage

@Composable
fun ItemGird(item: Item, onClick: (Item) -> Unit) {
    Surface(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.background
            )
            .height(100.dp)
            .width(80.dp),
            onClick = { onClick(item) },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            GameImage(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .fillMaxSize(),
                iconName = item.template.id
            )
            DefaultHorizontalDivider()
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = Humanizer.abbreviation(item.count),
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
@Preview
fun ItemGridPreview() {
    IdleGameTheme {
        ItemGird(item = PreviewConstants.testItem, onClick = {})
    }
}