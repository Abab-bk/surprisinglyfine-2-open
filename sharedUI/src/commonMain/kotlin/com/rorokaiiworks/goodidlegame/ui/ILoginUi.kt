package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.runtime.Composable
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

interface ILoginUi {
    @Composable
    fun LoginButton(
        onClick: () -> Unit,
        i18n: I18n = koinInject(),
        isLoading: Boolean
    )
}