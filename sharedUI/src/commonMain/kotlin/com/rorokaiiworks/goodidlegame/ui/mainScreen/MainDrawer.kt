
package com.rorokaiiworks.goodidlegame.ui.mainScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.composables.icons.feather.ChevronDown
import com.composables.icons.feather.ChevronRight
import com.composables.icons.feather.Feather
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.tutorial.TutorialRevealKey
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.GreatToken
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.CityScreenDestination
import com.rorokaiiworks.goodidlegame.ui.MainViewModel
import com.rorokaiiworks.goodidlegame.ui.mastery.MasteryLevelPanel
import com.rorokaiiworks.goodidlegame.ui.Notifier
import com.rorokaiiworks.goodidlegame.ui.commons.DefaultHorizontalDivider
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.commons.ProgressGameImage
import com.rorokaiiworks.goodidlegame.ui.skills.TaskSessionIndicator
import com.svenjacobs.reveal.RevealState
import com.svenjacobs.reveal.revealable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun MainDrawer(
    screens: List<AppDestination>,
    destination: AppDestination,
    i18n: I18n = koinInject(),
    viewModel: MainViewModel,
    scope: CoroutineScope,
    drawerState: DrawerState,
    revealState: RevealState,
    lockSkills: Boolean,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
    onNavigate: (AppDestination) -> Unit
) {
    val width = when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> 350.dp
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 290.dp
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 230.dp
        else -> 230.dp
    }

    Column(modifier = Modifier.width(width).fillMaxHeight()) {
        ModalDrawerSheet(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            drawerShape = RectangleShape,
        ) {
            MasteryLevelPanel(
                modifier = Modifier.padding(
                    all = 16.dp
                ),
                masteryLevel = viewModel.masteryLevel
            )

            val isCityActive = destination == AppDestination.CityDestination ||
                destination is AppDestination.CitySubDestination
            var isCityExpanded by remember { mutableStateOf(false) }
            val cityDestinations = remember {
                CityScreenDestination.entries.map { AppDestination.CitySubDestination(it) }
            }

            LaunchedEffect(isCityActive) {
                if (isCityActive) {
                    isCityExpanded = true
                }
            }

            var lastGroup: String? = null
            for (item in screens) {
                if (lastGroup != item.group) {
                    DefaultHorizontalDivider()
                    SeparationDrawerItem(
                        text = i18n.tr(item.group)
                    )
                    DefaultHorizontalDivider()
                }

                lastGroup = item.group

                if (item == AppDestination.CityDestination) {
                    NavigationDrawerItem(
                        modifier = Modifier
                            .revealable(key = item.route, state = revealState),
                        shape = RectangleShape,
                        label = {
                            DrawerItemLabel(
                                item = item,
                                playerSkills = viewModel.playerSkills
                            ) {
                                Icon(
                                    imageVector = if (isCityExpanded) Feather.ChevronDown else Feather.ChevronRight,
                                    contentDescription = null
                                )
                            }
                        },
                        selected = isCityActive,
                        onClick = { isCityExpanded = !isCityExpanded },
                    )

                    if (isCityExpanded) {
                        cityDestinations.forEach { cityItem ->
                            NavigationDrawerItem(
                                modifier = Modifier
                                    .padding(start = 20.dp)
                                    .revealable(key = TutorialRevealKey(cityItem.route), state = revealState),
                                shape = RectangleShape,
                                label = { CitySubDrawerItemLabel(cityItem) },
                                selected = destination == cityItem,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onNavigate(cityItem)
                                },
                            )
                        }
                    }

                    continue
                }

                val isUnlocked = item is AppDestination.SkillDestination &&
                        item.skillId in viewModel.gameState.unlockedSkills ||
                        !lockSkills

                NavigationDrawerItem(
                    modifier = Modifier
                        .revealable(key = item.route, state = revealState),
                    shape = RectangleShape,
                    label = {
                        if (item is AppDestination.SkillDestination && !isUnlocked)
                            LockedDrawerItemLabel()
                        else
                            DrawerItemLabel(item, viewModel.playerSkills) },
                    selected = destination == item,
                    onClick = {
                        if (item is AppDestination.SkillDestination && !isUnlocked) {
                            return@NavigationDrawerItem
                        }

                        scope.launch { drawerState.close() }
                        onNavigate(item)
                    },
                )
            }
        }

        viewModel.taskSystem.currentSession?.task?.let {
            TaskSessionIndicator(viewModel.taskSystem.currentSession!!)
        }
    }
}

@Composable
private fun CitySubDrawerItemLabel(
    item: AppDestination.CitySubDestination,
    i18n: I18n = koinInject(),
    greatToken: GreatToken = koinInject()
) {
    val greatTokenProgress = greatToken.totalProgress()
    val greatTokenFraction = if (greatTokenProgress.target > 0) {
        (greatTokenProgress.current.toFloat() / greatTokenProgress.target).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier.height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val title = i18n.tr(item.cityDestination.title)

        if (item.cityDestination == CityScreenDestination.GreatToken) {
            ProgressGameImage(
                modifier = Modifier.size(30.dp),
                iconName = item.route,
                progress = greatTokenFraction,
            )
        } else {
            GameImage(
                modifier = Modifier.size(30.dp),
                iconName = item.route
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = title,
        )
    }
}


@Composable
private fun SeparationDrawerItem(text: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(50.dp),
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            fontWeight = FontWeight.Bold,
        )
    }
}


@Composable
private fun LockedDrawerItemLabel(
    i18n: I18n = koinInject()
) {
    Text(
        text = i18n.tr("Locked"),
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun DrawerItemLabel(
    item: AppDestination,
    playerSkills: PlayerSkills,
    i18n: I18n = koinInject(),
    notifier: Notifier = koinInject(),
    content: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val title = i18n.tr(item.title)

        GameImage(
            modifier = Modifier.size(30.dp),
            iconName = item.route
        )

        Text(
            modifier = Modifier.weight(1f),
            text = title,
        )

        if (item is AppDestination.SkillDestination) {
            Text(
                text = "Lv. ${playerSkills.skills[item.skillId]?.level ?: 0}",
            )
        }

        val notification = notifier.badgeCountMap[item.route]

        notification?.let {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (notification.good) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = it.title,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        content?.let { it() }
    }
}
