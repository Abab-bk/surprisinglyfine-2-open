package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.ui.PreviewConstants
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun SkillActionInfoPanel(
    skillAction: SkillAction,
    i18n: I18n = koinInject(),
) {
    BaseCard {
        CardTitle(
            title = i18n.tr(skillAction.name),
        ) { Text(
            text = "Lv. ${skillAction.requiredLevel}",
            fontWeight = FontWeight.Bold,
        ) }
    }
}


@Composable
@Preview
private fun SkillActionInfoPanelPreview() {
    IdleGameTheme {
        SkillActionInfoPanel(PreviewConstants.testSkillAction)
    }
}
