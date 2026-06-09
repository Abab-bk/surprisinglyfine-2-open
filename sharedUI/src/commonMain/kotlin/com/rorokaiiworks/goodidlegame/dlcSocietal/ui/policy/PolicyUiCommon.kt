package com.rorokaiiworks.goodidlegame.dlcSocietal.ui.policy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyCard
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySlotType
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyUnlockCondition
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

internal val CARD_RADIUS = 4.dp
internal val CARD_SPACING = 10.dp

internal data class TypePalette(
    val container: Color,
    val border: Color,
    val accent: Color,
    val onAccent: Color,
)

@Composable
internal fun typePalette(
    type: PolicySlotType,
    isDark: Boolean,
): TypePalette {
    val cs = MaterialTheme.colorScheme
    val seed = when (type) {
        PolicySlotType.Economic -> Color(0xFF4C8BF5)
        PolicySlotType.Labor -> Color(0xFF2CB67D)
        PolicySlotType.Social -> Color(0xFFE85D75)
        PolicySlotType.Wildcard -> Color(0xFFF2A93B)
    }

    val containerBase = seed.harmonize(if (isDark) cs.surfaceContainerHighest else cs.surfaceContainer)
    val accent = seed.harmonize(if (isDark) cs.primaryContainer else cs.secondaryContainer)

    return TypePalette(
        container = containerBase,
        accent = accent,
        border = seed.harmonize(if (isDark) cs.outline else cs.outlineVariant),
        onAccent = if (isDark) cs.onPrimaryContainer else cs.onSecondaryContainer,
    )
}

@Composable
internal fun policyCardBrush(
    palette: TypePalette,
    isDark: Boolean,
): Brush {
    val top = palette.accent.copy(alpha = if (isDark) 0.44f else 0.28f)
    val middle = palette.container.copy(alpha = if (isDark) 0.30f else 0.18f)
    val bottom = palette.container.copy(alpha = if (isDark) 0.22f else 0.14f)
    return Brush.verticalGradient(colors = listOf(top, middle, bottom))
}

@Composable
internal fun policyScreenBackgroundBrush(isDark: Boolean): Brush {
    val cs = MaterialTheme.colorScheme
    val top = lerp(cs.surface, cs.primaryContainer, if (isDark) 0.08f else 0.12f)
    val middle = cs.surface
    val bottom = lerp(cs.surfaceContainerLow, cs.secondaryContainer, if (isDark) 0.06f else 0.10f)
    return Brush.verticalGradient(colors = listOf(top, middle, bottom))
}

@Composable
fun PolicyCardItem(
    policyCard: PolicyCard,
    isDark: Boolean,
    i18n: I18n = koinInject(),
    content: @Composable () -> Unit = {},
) {
    PolicyCardSurface(
        palette = typePalette(policyCard.slotType, isDark),
        isDark = isDark,
    ) {
        CardTitle(
            title = i18n.tr(policyCard.name)
        )

        Text(
            text = policyCard.displayText(i18n),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        content()
    }
}

@Composable
internal fun PolicyCardSurface(
    modifier: Modifier = Modifier,
    palette: TypePalette,
    isDark: Boolean,
    border: BorderStroke? = null,
    padding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(CARD_RADIUS)

    Surface(
        modifier = modifier.background(brush = policyCardBrush(palette = palette, isDark = isDark), shape = shape),
        shape = shape,
        color = Color.Transparent,
        border = border,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

