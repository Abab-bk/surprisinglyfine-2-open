package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.util.fastForEach

sealed interface ClickableStyleString

/**
 * A sealed interface representing different types of styled strings that can be used in a
 * [StyledText] composable. It includes simple styled text, clickable email links,
 * and clickable URL links.
 *
 * Each type contains the highlighted text, its style, and additional properties
 * for clickable types (email and URL).
 */
@Immutable
sealed interface StyledString {
    val highlightedText: String
    val style: SpanStyle

    /**
     * Represents a simple styled string without any clickable functionality.
     *
     * @param highlightedText The text to be highlighted.
     * @param style The style to be applied to the highlighted text.
     */
    @Immutable
    data class Simple(
        override val highlightedText: String,
        override val style: SpanStyle,
    ) : StyledString

    /**
     * Represents a clickable email link with a specific style.
     *
     * @param highlightedText The text to be highlighted.
     * @param email The email address that the link points to.
     * @param style The style to be applied to the highlighted text.
     */
    @Immutable
    data class ClickableEmail(
        override val highlightedText: String,
        val email: String,
        override val style: SpanStyle,
    ) : StyledString, ClickableStyleString

    /**
     * Represents a clickable URL link with a specific style.
     *
     * @param highlightedText The text to be highlighted.
     * @param url The URL that the link points to.
     * @param style The style to be applied to the highlighted text.
     */
    @Immutable
    data class ClickableUrl(
        override val highlightedText: String,
        val url: String,
        override val style: SpanStyle
    ) : StyledString, ClickableStyleString
}

/**
 * A composable function that displays styled text with clickable links.
 *
 * This function takes a full text string and a list of styled string types,
 * applying the specified styles and handling click events for clickable links.
 *
 * @param fullText The complete text to be displayed.
 * @param styledStrings A list of [StyledString] defining the styles and clickable links.
 * @param style The overall text style to be applied.
 * @param onClick A callback function that is invoked when a clickable link is clicked.
 * @param modifier An optional [Modifier] to apply to the Text composable.
 * @param color The color to be applied to the text. Defaults to [Color.Unspecified].
 * @param ignoreCase Whether to ignore case when searching for highlighted text.
 */
@Composable
fun StyledText(
    fullText: String,
    styledStrings: List<StyledString>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    onClick: (ClickableStyleString) -> Unit = {},
    ignoreCase: Boolean = false,
) {
    val annotatedString = rememberStyledAnnotationString(
        fullText = fullText,
        styledStrings = styledStrings,
        ignoreCase = ignoreCase,
        onClick = onClick
    )

    Text(
        modifier = modifier,
        text = annotatedString,
        style = style,
        color = color
    )
}

/**
 * Creates an [AnnotatedString] from the provided full text and styled string types.
 *
 * This function builds an annotated string that applies styles and clickable links
 * based on the specified [StyledString]s.
 *
 * @param fullText The complete text to be processed.
 * @param styledStrings A list of [StyledString] defining the styles and clickable links.
 * @param ignoreCase Whether to ignore case when searching for highlighted text.
 * @param onClick A callback function that is invoked when a clickable link is clicked.
 * @return An [AnnotatedString] with the applied styles and clickable links.
 */
@Composable
fun rememberStyledAnnotationString(
    fullText: String,
    styledStrings: List<StyledString>,
    ignoreCase: Boolean = false,
    onClick: (ClickableStyleString) -> Unit
): AnnotatedString {
    val currentOnClick by rememberUpdatedState(onClick)

    return remember(fullText, styledStrings, ignoreCase) {
        buildAnnotatedString {
            append(fullText)

            styledStrings.fastForEach { styledStringInfo ->
                val indices = fullText.findAllOccurrences(
                    substring = styledStringInfo.highlightedText,
                    ignoreCase = ignoreCase
                )

                indices.fastForEach { startIndex ->
                    applyStyle(
                        styledString = styledStringInfo,
                        startIndex = startIndex,
                        endIndex = startIndex + styledStringInfo.highlightedText.length,
                        onClick = currentOnClick
                    )
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.applyStyle(
    styledString: StyledString,
    startIndex: Int,
    endIndex: Int,
    onClick: (ClickableStyleString) -> Unit
) {
    when (styledString) {
        is StyledString.ClickableUrl -> {
            val linkAnnotation = LinkAnnotation.Url(
                url = styledString.url,
                styles = TextLinkStyles(style = styledString.style),
                linkInteractionListener = { onClick(styledString) }
            )
            addLink(linkAnnotation, startIndex, endIndex)
        }

        is StyledString.ClickableEmail -> {
            val linkAnnotation = LinkAnnotation.Clickable(
                tag = styledString.highlightedText,
                styles = TextLinkStyles(style = styledString.style),
                linkInteractionListener = { onClick(styledString) }
            )
            addLink(linkAnnotation, startIndex, endIndex)
        }

        is StyledString.Simple -> {
            addStyle(style = styledString.style, start = startIndex, end = endIndex)
        }
    }
}

/**
 * Find all occurrences of a substring in a string, optionally ignoring case.
 *
 * @param substring The substring to search for.
 * @param ignoreCase Whether to perform a case-insensitive search.
 * @return A list of indices where the substring was found.
 */
private fun String.findAllOccurrences(substring: String, ignoreCase: Boolean = false): List<Int> {
    if (substring.isEmpty()) return emptyList()

    val indices = mutableListOf<Int>()
    val searchString = if (ignoreCase) this.lowercase() else this
    val searchSubstring = if (ignoreCase) substring.lowercase() else substring

    var startIndex = 0
    val maxStartIndex = length - substring.length

    while (startIndex <= maxStartIndex) {
        val index = searchString.indexOf(searchSubstring, startIndex)
        if (index == -1) break
        indices.add(index)
        startIndex = index + 1
    }

    return indices.toList()
}