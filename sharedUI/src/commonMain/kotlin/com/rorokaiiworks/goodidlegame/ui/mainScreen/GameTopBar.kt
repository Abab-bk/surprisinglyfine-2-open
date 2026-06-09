package com.rorokaiiworks.goodidlegame.ui.mainScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Gift
import com.composables.icons.feather.Menu
import com.composables.icons.feather.TrendingUp
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.CityTopBar
import com.rorokaiiworks.goodidlegame.ui.commons.TopBarLabel
import com.rorokaiiworks.goodidlegame.ui.persistent.SaveIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(
    scope: CoroutineScope,
    drawerState: DrawerState,
    destination: AppDestination,
    showDrawerButton: Boolean,
    i18n: I18n = koinInject(),
    actions: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    showAdOpportunityButton: Boolean,
    onAdOpportunityClick: () -> Unit,
    effectsClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            if (showDrawerButton) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } }
                ) {
                    Icon(
                        imageVector = Feather.Menu,
                        contentDescription = "Open drawer"
                    )
                }
            }
        },

        actions = {
            // TODO 接入广告后启用
//                    if (showAdOpportunityButton) {
//                        AdOpportunityIcon(
//                            onClick = onAdOpportunityClick
//                        )
//                    }

            actions()

            SaveIcon()

            Spacer(
                modifier = Modifier.width(16.dp)
            )
            
            TopBarLabel(
                modifier = Modifier.clickable { effectsClick() }
            ) {
                Icon(
                    modifier = Modifier.size(26.dp),
                    imageVector = Feather.TrendingUp,
                    contentDescription = "Effects"
                )
            }
        },

        title = {
            if (destination == AppDestination.CityDestination ||
                destination is AppDestination.CitySubDestination
            ) {
                CityTopBar()
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = i18n.tr(destination.title)
                    )

                    title()
                }
            }
        }
    )
}


@Composable
private fun AdOpportunityIcon(
    onClick: () -> Unit
) {
    TopBarLabel(
        modifier = Modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Icon(
            imageVector = Feather.Gift,
            contentDescription = "Ad Opportunity",
        )
    }
}
