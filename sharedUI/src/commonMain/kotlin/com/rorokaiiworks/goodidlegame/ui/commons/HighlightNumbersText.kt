package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.Constants

@Composable
fun HighlightNumbers(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign? = null,
    color: Color = Color.Unspecified,
) {
    val annotatedString = buildAnnotatedString {
        for (char in text) {
            if (char.isLetter()) {
                append(char)
                continue
            }

            withStyle(style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )) {
                append(char)
            }
        }
    }

    Text(
        modifier = modifier,
        text = annotatedString,
        color = color,
        textAlign = textAlign,
    )
}


@Composable
fun HighlightTextLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.background
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
    ) {
        HighlightNumbers(
            modifier = Modifier.padding(6.dp),
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}