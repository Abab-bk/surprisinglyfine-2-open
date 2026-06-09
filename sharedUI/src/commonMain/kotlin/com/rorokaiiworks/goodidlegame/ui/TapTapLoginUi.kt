package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import goodidlegame.sharedui.generated.resources.Res
import goodidlegame.sharedui.generated.resources.taptap_login_btn
import name.kropp.kotlinx.gettext.I18n
import org.jetbrains.compose.resources.painterResource

class TapTapLoginUi : ILoginUi {
    @Composable
    override fun LoginButton(
        onClick: () -> Unit,
        i18n: I18n,
        isLoading: Boolean
    ) {
        Image(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable {
                if (!isLoading) {
                    onClick()
                }
            },
            painter = painterResource(Res.drawable.taptap_login_btn),
            contentDescription = null
        )
    }
}