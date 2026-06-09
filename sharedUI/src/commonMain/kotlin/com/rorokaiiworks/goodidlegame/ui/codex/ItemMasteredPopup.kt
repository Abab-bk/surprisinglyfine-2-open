package com.rorokaiiworks.goodidlegame.ui.codex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.core.FakeI18n
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import kotlinx.coroutines.delay
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ItemMasteredPopup(
    i18n: I18n = koinInject(),
    itemTemplate: ItemTemplate,
    onClick: () -> Unit = {},
    onReady: () -> Unit = {},
    initialVisible: Boolean = false
) {
    var visible by remember { mutableStateOf(initialVisible) }
    val infiniteTransition = rememberInfiniteTransition()
    val primaryColor = MaterialTheme.colorScheme.primary

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        onReady()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(180.dp, 30.dp)) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    primaryColor.copy(alpha = glowAlpha * 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    Text(
                        text = i18n.tr("Item Mastered"),
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryColor,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        drawCircle(
                            color = primaryColor.copy(alpha = rippleAlpha * 0.3f),
                            radius = size.width / 2 * rippleScale,
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = rippleAlpha * 0.2f),
                            radius = size.width / 2 * rippleScale * 0.85f,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    Canvas(modifier = Modifier.size(160.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = glowAlpha * 0.2f),
                                    primaryColor.copy(alpha = glowAlpha * 0.1f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width / 2
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.5f.dp,
                                color = primaryColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GameImage(
                            iconName = itemTemplate.id,
                            modifier = Modifier.size(70.dp)
                        )
                    }

                    val cornerOffset = 60.dp
                    listOf(
                        Offset(-1f, -1f),
                        Offset(1f, -1f),
                        Offset(-1f, 1f),
                        Offset(1f, 1f)
                    ).forEach { (dx, dy) ->
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = cornerOffset * dx,
                                    y = cornerOffset * dy
                                )
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = glowAlpha))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = i18n.tr(itemTemplate.name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = i18n.tr("Mastery Complete"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun MasteryLevelUpPopup(
    i18n: I18n = koinInject(),
    level: Int,
    onClick: () -> Unit = {},
    onReady: () -> Unit = {},
    initialVisible: Boolean = false
) {
    var visible by remember { mutableStateOf(initialVisible) }
    val infiniteTransition = rememberInfiniteTransition()
    val primaryColor = MaterialTheme.colorScheme.primary

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val rayScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        onReady()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(200.dp, 30.dp)) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    primaryColor.copy(alpha = glowAlpha * 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    Text(
                        text = i18n.tr("Mastery Rank"),
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryColor,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(240.dp)) {
                        drawCircle(
                            color = primaryColor.copy(alpha = rippleAlpha * 0.3f),
                            radius = size.width / 2 * rippleScale,
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = rippleAlpha * 0.25f),
                            radius = size.width / 2 * rippleScale * 0.85f,
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = rippleAlpha * 0.2f),
                            radius = size.width / 2 * rippleScale * 0.7f,
                            style = Stroke(width = 1f)
                        )
                    }

                    Canvas(modifier = Modifier.size(200.dp)) {
                        for (i in 0..7) {
                            val angle = (i * 45f) * (PI / 180f).toFloat()
                            val startRadius = size.width / 3
                            val endRadius = startRadius + (size.width / 6 * rayScale)
                            drawLine(
                                color = primaryColor.copy(alpha = glowAlpha * 0.4f),
                                start = Offset(
                                    center.x + cos(angle) * startRadius,
                                    center.y + sin(angle) * startRadius
                                ),
                                end = Offset(
                                    center.x + cos(angle) * endRadius,
                                    center.y + sin(angle) * endRadius
                                ),
                                strokeWidth = 2f
                            )
                        }
                    }

                    Canvas(modifier = Modifier.size(220.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = glowAlpha * 0.25f),
                                    primaryColor.copy(alpha = glowAlpha * 0.12f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.width / 2
                            )
                        )
                    }

                    Canvas(modifier = Modifier.size(180.dp).rotate(rotation)) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.15f),
                            style = Stroke(width = 1f)
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.1f),
                            radius = size.width / 2 - 12,
                            style = Stroke(width = 1f)
                        )
                        for (i in 0..3) {
                            val angle = (i * 90f) * (PI / 180f).toFloat()
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.3f),
                                radius = 3f,
                                center = Offset(
                                    center.x + cos(angle) * (size.width / 2),
                                    center.y + sin(angle) * (size.width / 2)
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 2.dp,
                                color = primaryColor.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "RANK",
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryColor.copy(alpha = 0.8f),
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$level",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 56.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = i18n.tr("Rank Advanced"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = i18n.tr("Your mastery continues to grow"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


@Preview
@Composable
private fun ItemMasteredPopupPreview() {
    IdleGameTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ItemMasteredPopup(
                i18n = FakeI18n(),
                itemTemplate =
                    ItemTemplate(
                        id = "pine_wood",
                        name = "Pine Wood"
                    ),
                initialVisible = true
            )
        }
    }
}
