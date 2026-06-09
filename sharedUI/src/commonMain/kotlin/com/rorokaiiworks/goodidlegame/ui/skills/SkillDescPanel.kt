package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitleWithCloseBtn

@Composable
fun SkillDescPanel(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    onClose: () -> Unit
) {
    BaseCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardTitleWithCloseBtn(title = title, onClose = onClose)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            text = desc,
            lineHeight = 32.sp
        )
    }
}
