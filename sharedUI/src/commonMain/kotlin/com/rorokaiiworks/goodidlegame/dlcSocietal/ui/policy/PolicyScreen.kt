package com.rorokaiiworks.goodidlegame.dlcSocietal.ui.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PolicyScreen(
    viewModel: PolicyScreenViewModel = koinViewModel(),
    settingsSaver: SettingsSaver = koinInject(),
    i18n: I18n = koinInject()
) {
    val settings by settingsSaver.settings.collectAsState()
    val isDark = settings.themePreference.isDark()
    val wideScreen = isWideScreen()
    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(policyScreenBackgroundBrush(isDark)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cs.surfaceContainerLow,
            ) {
                SecondaryTabRow(
                    selectedTabIndex = viewModel.currentPage.index,
                    containerColor = Color.Transparent,
                ) {
                    Tab(
                        selected = viewModel.currentPage.index == PolicyScreenPage.Loadout.index,
                        onClick = { viewModel.currentPage = PolicyScreenPage.Loadout },
                        text = { Text(i18n.tr("Loadout")) },
                    )
                    Tab(
                        selected = viewModel.currentPage.index == PolicyScreenPage.Shop.index,
                        onClick = { viewModel.currentPage = PolicyScreenPage.Shop },
                        text = { Text(i18n.tr("Shop")) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (viewModel.currentPage) {
                    PolicyScreenPage.Loadout -> PolicyLoadoutTab(
                        viewModel = viewModel,
                        isDark = isDark,
                        wideScreen = wideScreen,
                    )

                    PolicyScreenPage.Shop -> PolicyShopTab(
                        viewModel = viewModel,
                        isDark = isDark,
                    )
                }
            }
        }
    }
}
