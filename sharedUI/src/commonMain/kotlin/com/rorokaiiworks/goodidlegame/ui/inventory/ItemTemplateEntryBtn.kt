package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemRarity
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private class LegendaryBackgroundNode(
    var isDark: Boolean,
    private val animationState: Animatable<Float, AnimationVector1D>
) : Modifier.Node(), DrawModifierNode {
    companion object {
        val colors = listOf(
            Color(0xFFFF0080), Color(0xFFFF4500), Color(0xFFFFA500),
            Color(0xFFFFD700), Color(0xFF00FF88), Color(0xFF00BFFF),
            Color(0xFF923FE5), Color(0xFFFF0080),
        )
    }

    override fun ContentDrawScope.draw() {
        val rotation = animationState.value
        val angleRad = Math.toRadians(rotation.toDouble())
        val cos = cos(angleRad).toFloat()
        val sin = sin(angleRad).toFloat()

        val r = sqrt(size.width * size.width + size.height * size.height) / 2

        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(center.x - cos * r, center.y - sin * r),
                end = Offset(center.x + cos * r, center.y + sin * r)
            )
        )

        val overlayColor = if (isDark) Color.Black else Color.White
        drawRect(color = overlayColor.copy(alpha = 0.5f))

        drawContent()
    }
}

private data class LegendaryBackgroundElement(
    val isDark: Boolean,
    val animationState: Animatable<Float, AnimationVector1D>
) : ModifierNodeElement<LegendaryBackgroundNode>() {

    override fun create(): LegendaryBackgroundNode =
        LegendaryBackgroundNode(isDark, animationState)

    override fun update(node: LegendaryBackgroundNode) {
        node.isDark = isDark
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "legendaryBackground"
        properties["isDark"] = isDark
    }
}

private const val overlayAlpha = 0.7f

fun Modifier.legendaryBackground(isDark: Boolean): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "Legendary")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    val animatable = remember { Animatable(0f) }
    LaunchedEffect(rotation.value) {
        animatable.snapTo(rotation.value)
    }

    this.then(LegendaryBackgroundElement(isDark, animatable))
}

private fun commonBackground(isDark: Boolean): Modifier = Modifier.drawWithContent {
    val overlayColor = if (isDark) Color.Black else Color.White
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFFC5C2C2)),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
    )
    drawRect(color = overlayColor.copy(alpha = overlayAlpha))
    drawContent()
}

private fun uncommonBackground(isDark: Boolean): Modifier = Modifier.drawWithContent {
    val overlayColor = if (isDark) Color.Black else Color.White
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF1B5E20), Color(0xFF43A047), Color(0xFF76FF03)),
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f)
        )
    )
    drawRect(color = overlayColor.copy(alpha = overlayAlpha))
    drawContent()
}

private fun rareBackground(isDark: Boolean): Modifier = Modifier.drawWithContent {
    val overlayColor = if (isDark) Color.Black else Color.White
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF40C4FF)),
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f)
        )
    )
    drawRect(color = overlayColor.copy(alpha = overlayAlpha))
    drawContent()
}

private fun epicBackground(isDark: Boolean): Modifier = Modifier.drawWithContent {
    val overlayColor = if (isDark) Color.Black else Color.White
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFFCE93D8)),
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f)
        )
    )
    drawRect(color = overlayColor.copy(alpha = overlayAlpha))
    drawContent()
}

private fun mythBackground(isDark: Boolean): Modifier = Modifier.drawWithContent {
    val overlayColor = if (isDark) Color.Black else Color.White
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB71C1C), Color(0xFFE53935),
                Color(0xFFFFD700), Color(0xFFFFF176),
                Color(0xFFFFD700), Color(0xFFE53935),
            ),
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f)
        )
    )
    drawRect(color = overlayColor.copy(alpha = overlayAlpha))
    drawContent()
}

fun Modifier.rarityBackground(rarity: ItemRarity, isDark: Boolean): Modifier = when (rarity) {
    ItemRarity.Common -> this.then(commonBackground(isDark))
    ItemRarity.Uncommon -> this.then(uncommonBackground(isDark))
    ItemRarity.Rare -> this.then(rareBackground(isDark))
    ItemRarity.Epic -> this.then(epicBackground(isDark))
    ItemRarity.Legendary -> legendaryBackground(isDark)
    ItemRarity.Myth -> this.then(mythBackground(isDark))
}


@Composable
fun selectedItemEntryBorder(): BorderStroke {
    return BorderStroke(
        width = 2.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
}


@Composable
fun ItemTemplateEntry(
    modifier: Modifier? = null,
    itemTemplate: ItemTemplate,
    onClick: () -> Unit = { },
    i18n: I18n = koinInject(),
    settingsSaver: SettingsSaver = koinInject(),
    nameOverride: String? = null,
    nameColor: Color? = null,
    isSelected: Boolean = false,
    columnContent: @Composable () -> Unit = { },
    titleLabel: @Composable () -> Unit = { },
    content: @Composable () -> Unit = { }
) {
    Surface(
        color = Color.Transparent,
        modifier = (modifier ?: Modifier.fillMaxWidth().heightIn(min = 48.dp))
            .then(Modifier.rarityBackground(itemTemplate.rarity, settingsSaver.settings.value.themePreference.isDark())),
        shape = RectangleShape,
        onClick = onClick,
        border = if (isSelected) selectedItemEntryBorder() else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameImage(Modifier.size(30.dp), itemTemplate.id)

                    if (nameColor != null) {
                        Text(
                            text = nameOverride ?: i18n.tr(itemTemplate.name),
                            color = nameColor,
                        )
                    } else {
                        Text(
                            text = nameOverride ?: i18n.tr(itemTemplate.name),
                        )
                    }

                    titleLabel()
                }
                content()
            }

            if (itemTemplate.perk != null) {
                Text(text = i18n.tr(itemTemplate.perk.desc), style = MaterialTheme.typography.bodySmall)
            }
            columnContent()
        }
    }
}


@Composable
fun ItemEntryPanel(
    modifier: Modifier = Modifier,
    item: Item,
    isSelected: Boolean,
    nameOverride: String? = null,
    onClick: () -> Unit = { },
    content: @Composable () -> Unit = { }
) {
    ItemTemplateEntry(
        modifier = modifier,
        itemTemplate = item.template,
        isSelected = isSelected,
        columnContent = {
            ItemModifiersPanel(
                modifiers = item.allModifiers
            )
        },
        nameOverride = nameOverride,
        onClick = onClick,
        content = content
    )
}


@Composable
fun EntrySurface(
    modifier: Modifier = Modifier,
    iconName: String?,
    title: String?,
    content: @Composable () -> Unit = { },
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth().heightIn(
            min = 48.dp,
        ),
        shape = RectangleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                iconName?.let {
                    GameImage(
                        modifier = Modifier.size(30.dp),
                        iconName = it
                    )
                }

                title?.let {
                    Text(
                        text = it,
                    )
                }
            }

            content()
        }
    }
}